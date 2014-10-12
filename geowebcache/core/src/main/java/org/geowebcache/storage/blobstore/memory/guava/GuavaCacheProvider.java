/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geowebcache.storage.blobstore.memory.guava;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration;
import org.geowebcache.storage.blobstore.memory.CacheProvider;
import org.geowebcache.storage.blobstore.memory.CacheStatistics;
import org.geowebcache.storage.blobstore.memory.NullBlobStore;
import org.geowebcache.storage.blobstore.memory.CacheConfiguration.EvictionPolicy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;

/**
 * This class is an implementation of the {@link CacheProvider} interface using a backing Guava {@link Cache} object. This implementation requires to
 * be configured with the setConfiguration() method and to be modified by calling resetCache() and then setting the new configuration.
 * 
 * @author Nicola Lagomarsini Geosolutions
 */
public class GuavaCacheProvider implements CacheProvider {

    /** {@link Logger} object used for logging exceptions */
    private final static Log LOGGER = LogFactory.getLog(NullBlobStore.class);

    /** Separator char used for creating Cache keys */
    public final static String SEPARATOR = "_";

    /** Constant for multiplying bytes to MB */
    public final static long BYTES_TO_MB = 1048576;

    /**
     * This class handles the {@link CacheStats} object returned by the guava cache.
     * 
     * @author Nicola Lagomarsini Geosolutions
     */
    public static class GuavaCacheStatistics extends CacheStatistics {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        public GuavaCacheStatistics(CacheStats stats) {
            this.setEvictionCount(stats.evictionCount());
            this.setHitCount(stats.hitCount());
            this.setMissCount(stats.missCount());
            this.setTotalCount(stats.requestCount());
            this.setHitRate((int) (stats.hitRate() * 100));
            this.setMissRate(100 - getHitRate());
        }
    }

    /** Cache object containing the various {@link TileObject}s */
    private Cache<String, TileObject> cache;

    /** Internal Multimap used for storing the TileObject ids associated to each cached Layer */
    private LayerMap multimap;

    /** {@link AtomicBoolean} used for ensuring that the Cache has already been configured */
    private AtomicBoolean configured;

    /** {@link AtomicLong} used for checking the number of active operations to wait when resetting the cache */
    private AtomicLong actualOperations;

    /** Internal concurrent Set used for saving the names of the Layers that must not be cached */
    private final ConcurrentSkipListSet<String> layers;

    public GuavaCacheProvider() {
        // Initialization of the Layer set and of the Atomic parameters
        layers = new ConcurrentSkipListSet<String>();
        configured = new AtomicBoolean(false);
        actualOperations = new AtomicLong(0);
    }

    /**
     * This method is used for creating a new cache object, from the defined configuration.
     * 
     * @param configuration
     */
    private void initCache(CacheConfiguration configuration) {
        // Initialization step
        int concurrency = configuration.getConcurrencyLevel();
        long maxMemory = configuration.getHardMemoryLimit() * BYTES_TO_MB;
        long evictionTime = configuration.getEvictionTime();
        EvictionPolicy policy = configuration.getPolicy();

        // If Cache already exists, flush it
        if (cache != null) {
            cache.invalidateAll();
        }
        // Create the CacheBuilder
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        // Add parameters
        CacheBuilder<String, TileObject> newBuilder = builder.maximumWeight(maxMemory)
                .recordStats().weigher(new Weigher<String, TileObject>() {

                    @Override
                    public int weigh(String key, TileObject value) {
                        return value.getBlobSize();
                    }
                }).concurrencyLevel(concurrency)
                .removalListener(new RemovalListener<String, TileObject>() {

                    @Override
                    public void onRemoval(RemovalNotification<String, TileObject> notification) {
                        // TODO This operation is not atomic
                        TileObject obj = notification.getValue();
                        final String tileKey = generateTileKey(obj);
                        final String layerName = obj.getLayerName();
                        multimap.removeTile(layerName, tileKey);
                        if(LOGGER.isDebugEnabled()){
                            LOGGER.debug("Removed tile "+tileKey+" for layer "+layerName+ " due to reason:"+notification.getCause().toString());
                            LOGGER.debug("Removed tile was evicted? "+notification.wasEvicted());
                        }
                    }
                });
        // Handle eviction policy
        if (policy != null && evictionTime > 0) {
            if (policy == EvictionPolicy.EXPIRE_AFTER_ACCESS) {
                newBuilder.expireAfterAccess(evictionTime, TimeUnit.SECONDS);
            } else if (policy == EvictionPolicy.EXPIRE_AFTER_WRITE) {
                newBuilder.expireAfterWrite(evictionTime, TimeUnit.SECONDS);
            }
        }

        // Build the cache
        cache = newBuilder.build();

        // Created a new multimap
        multimap = new LayerMap();

        // Update the configured parameter
        configured.getAndSet(true);
    }

    @Override
    public synchronized void setConfiguration(CacheConfiguration configuration) {
        // NOTE that if the cache has already been configured, the user must always call resetCache() before
        // setting the new configuration
        if (!configured.get()) {
            // Configure a new cache
            initCache(configuration);
        }
    }

    @Override
    public TileObject getTileObj(TileObject obj) {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Check if the layer must be cached
                if (layers.contains(obj.getLayerName())) {
                    // The layer must not be cached
                    return null;
                }
                // Generate the TileObject key
                String id = generateTileKey(obj);
                // Get the key from the cache
                return cache.getIfPresent(id);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
        return null;
    }

    @Override
    public void putTileObj(TileObject obj) {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Check if the layer must be cached
                if (layers.contains(obj.getLayerName())) {
                    // The layer must not be cached
                    return;
                }
                // Generate the TileObject key
                String id = generateTileKey(obj);
                // Add the TileObject to the cache and its id in the multimap
                cache.put(id, obj);
                multimap.putTile(obj.getLayerName(), id);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void removeTileObj(TileObject obj) {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Check if the layer must be cached
                if (layers.contains(obj.getLayerName())) {
                    // The layer must not be cached
                    return;
                }
                // Generate the TileObject key
                String id = generateTileKey(obj);
                // Remove the key
                cache.invalidate(id);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void removeLayer(String layername) {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Check if the layer must be cached
                if (layers.contains(layername)) {
                    // The layer must not be cached
                    return;
                }
                // Get all the TileObject ids associated to the Layer and removes them
                Set<String> keys = multimap.removeLayer(layername);
                if (keys != null) {
                    cache.invalidateAll(keys);
                }
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void clear() {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Remove all the elements from the cache
                if (cache != null) {
                    cache.invalidateAll();
                }
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void reset() {
        if (configured.getAndSet(false)) {
            // Avoid to call the While cycle before having started an operation with configured == false
            actualOperations.incrementAndGet();
            actualOperations.decrementAndGet();
            // Wait until all the operations are finished
            while (actualOperations.get() > 0) {
            }
            // Remove all the elements from the cache
            if (cache != null) {
                cache.invalidateAll();
            }
            // Remove all the Layers configured for avoiding caching
            layers.clear();
        }
    }

    @Override
    public CacheStatistics getStatistics() {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Returns a new Object containing a snapshot of the cache statistics
                return new GuavaCacheStatistics(cache.stats());
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        } else {
            // Else returns an empty CacheStatistics object
            return new CacheStatistics();
        }
    }

    /***
     * Static method for generating the {@link TileObject} cache key to use for caching.
     * 
     * @param obj
     * 
     * @return {@link TileObject} key
     */
    public static String generateTileKey(TileObject obj) {
        return new StringBuilder(obj.getLayerName()).append(SEPARATOR).append(obj.getGridSetId() ).append( SEPARATOR)
                .append( Arrays.toString(obj.getXYZ()) ).append( SEPARATOR ).append( obj.getBlobFormat()).toString();
    }

    @Override
    public void addUncachedLayer(String layername) {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Adds the layer which should not be cached
                layers.add(layername);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void removeUncachedLayer(String layername) {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Configure a Layer for being cached again
                layers.remove(layername);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public boolean containsUncachedLayer(String layername) {
        // Check if the cache has already been configured
        if (configured.get()) {
            // Increment the number of current operations
            // This behavior is used in order to wait
            // the end of all the operations after setting
            // the configured parameter to false
            actualOperations.incrementAndGet();
            try {
                // Check if the layer must not be cached
                return layers.contains(layername);
            } finally {
                // Decrement the number of current operations.
                actualOperations.decrementAndGet();
            }
        } else {
            return false;
        }
    }

    /**
     * Internal class representing a concurrent multimap which associates to each Layer name the related {@link TileObject} cache keys. This map is
     * useful when trying to remove a Layer, because it returns quicly all the cached keys of the selected layer, without having to cycle on the cache
     * and checking if each TileObject belongs to the selected Layer.
     * 
     * @author Nicola Lagomarsini, GeoSolutions
     * 
     */
    static class LayerMap {

        /** {@link ReentrantReadWriteLock} used for handling concurrency */
        private final ReentrantReadWriteLock lock;

        /** {@link WriteLock} used when trying to change the map */
        private final WriteLock writeLock;

        /** {@link ReadLock} used when accessing the map */
        private final ReadLock readLock;

        /** MultiMap containing the {@link TileObject} keys for the Layers */
        private final ConcurrentHashMap<String, Set<String>> layerMap = new ConcurrentHashMap<String, Set<String>>();

        public LayerMap() {
            // Lock initialization
            lock = new ReentrantReadWriteLock(true);
            writeLock = lock.writeLock();
            readLock = lock.readLock();
        }

        /**
         * Insertion of a {@link TileObject} key in the map for the associated Layer.
         * 
         * @param layer
         * @param id
         */
        public void putTile(String layer, String id) {
            // ReadLock is used because we are only accessing the map
            readLock.lock();
            Set<String> tileKeys = layerMap.get(layer);
            if (tileKeys == null) {
                // If the Map is not present, we must add it
                // So we do the unlock and try to acquire the writeLock
                readLock.unlock();
                writeLock.lock();
                try {
                    // Check again if the tileKey has not been added already
                    tileKeys = layerMap.get(layer);
                    if (tileKeys == null) {
                        // If no key is present then a new KeySet is created and then added to the multimap
                        tileKeys = new ConcurrentSkipListSet<String>();
                        layerMap.put(layer, tileKeys);
                    }
                    // Downgrade by acquiring read lock before releasing write lock
                    readLock.lock();
                } finally {
                    // Release the writeLock
                    writeLock.unlock();
                }
            }
            try {
                // Finally the tile key is added.
                tileKeys.add(id);
            } finally {
                readLock.unlock();
            }
        }

        /**
         * Removal of a {@link TileObject} key in the map for the associated Layer.
         * 
         * @param layer
         * @param id
         */
        public void removeTile(String layer, String id) {
            // ReadLock is used because we are only accessing the map
            readLock.lock();
            try {
                // KeySet associated to the image
                Set<String> tileKeys = layerMap.get(layer);
                if (tileKeys != null) {
                    // Removal of the keys
                    tileKeys.remove(id);
                    // If the KeySet is empty then it is removed from the multimap
                    if (tileKeys.isEmpty()) {
                        readLock.unlock();
                        writeLock.lock();
                        try {
                            if (tileKeys.isEmpty()) {
                                // Here writeLock is acquired again, but it is reentrant
                                removeLayer(layer);
                            }
                            // Downgrade by acquiring read lock before releasing write lock
                            readLock.lock();
                        } finally {
                            writeLock.unlock();
                        }
                    }
                }
            } finally {
                readLock.unlock();
            }
        }

        /**
         * Removes a layer {@link Set} and returns it to the cache.
         * 
         * @param layer
         * 
         * @return the keys associated to the Layer
         */
        public Set<String> removeLayer(String layer) {
            writeLock.lock();
            try {
                // Get the Set from the map
                Set<String> layers = layerMap.get(layer);
                // Removes the set from the map
                layerMap.remove(layer);
                // Returns the set
                return layers;
            } finally {
                writeLock.unlock();
            }
        }
    }
}