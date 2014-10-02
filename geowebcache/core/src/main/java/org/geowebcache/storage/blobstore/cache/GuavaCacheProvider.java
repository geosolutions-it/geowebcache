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
package org.geowebcache.storage.blobstore.cache;

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

import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.blobstore.cache.CacheConfiguration.EvictionPolicy;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.Weigher;

/**
 * @author Nicola Lagomarsini Geosolutions
 */
public class GuavaCacheProvider implements CacheProvider {

    /** Separator char used for creating Cache keys */
    public final static String SEPARATOR = "_";
    
    public final static long BYTES_TO_MB = 1048576;

    public static class GuavaCacheStatistics extends CacheStatistics {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        public GuavaCacheStatistics(CacheStats stats) {
            this.setEvictionCount(stats.evictionCount());
            this.setHitCount(stats.hitCount());
            this.setMissCount(stats.missCount());
            this.setTotalCount(stats.requestCount());
            this.setHitRate((int)(stats.hitRate()*100));
            this.setMissRate(100 - getHitRate());
        }
    }

    /** Cache object containing the various {@link TileObject}s */
    private Cache<String, TileObject> cache;

    private LayerMap multimap;

    private AtomicBoolean configured;

    private AtomicLong actualOperations;

    private final ConcurrentSkipListSet<String> layers;

    public GuavaCacheProvider() {
        layers = new ConcurrentSkipListSet<String>();
        configured = new AtomicBoolean(false);
        actualOperations = new AtomicLong(0);
    }

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
                        multimap.removeTile(obj.getLayerName(), generateTileKey(obj));
                    }
                });
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
        if (!configured.get()) {
            initCache(configuration);
        }
    }

    @Override
    public TileObject getTileObj(TileObject obj) {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                if (layers.contains(obj.getLayerName())) {
                    return null;
                }
                String id = generateTileKey(obj);
                return cache.getIfPresent(id);
            } finally {
                actualOperations.decrementAndGet();
            }
        }
        return null;
    }

    @Override
    public void putTileObj(TileObject obj) {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                if (layers.contains(obj.getLayerName())) {
                    return;
                }
                String id = generateTileKey(obj);
                // TODO This operation is not atomic
                cache.put(id, obj);
                multimap.putTile(obj.getLayerName(), id);
            } finally {
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void removeTileObj(TileObject obj) {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                if (layers.contains(obj.getLayerName())) {
                    return;
                }
                String id = generateTileKey(obj);
                cache.invalidate(id);
            } finally {
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void removeLayer(String layername) {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                if (layers.contains(layername)) {
                    return;
                }
                Set<String> keys = multimap.removeLayer(layername);
                if (keys != null) {
                    cache.invalidateAll(keys);
                }
            } finally {
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void clearCache() {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                if (cache != null) {
                    cache.invalidateAll();
                }
            } finally {
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void resetCache() {
        if (configured.getAndSet(false)) {
            // Avoid to call the While cycle before having started an operation with configured == true
            actualOperations.incrementAndGet();
            actualOperations.decrementAndGet();
            // Wait until all the operations are finished
            while (actualOperations.get() > 0) {
            }
            if (cache != null) {
                cache.invalidateAll();
            }
            layers.clear();
        }
    }

    @Override
    public CacheStatistics getStats() {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                return new GuavaCacheStatistics(cache.stats());
            } finally {
                actualOperations.decrementAndGet();
            }
        } else {
            return new CacheStatistics();
        }
    }

    public static String generateTileKey(TileObject obj) {
        return obj.getLayerName() + SEPARATOR + obj.getGridSetId() + SEPARATOR
                + Arrays.toString(obj.getXYZ()) + SEPARATOR + obj.getBlobFormat();
    }

    @Override
    public void addUncachedLayer(String layername) {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                layers.add(layername);
            } finally {
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public void removeUncachedLayer(String layername) {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                layers.remove(layername);
            } finally {
                actualOperations.decrementAndGet();
            }
        }
    }

    @Override
    public boolean containsUncachedLayer(String layername) {
        if (configured.get()) {
            actualOperations.incrementAndGet();
            try {
                return layers.contains(layername);
            } finally {
                actualOperations.decrementAndGet();
            }
        } else {
            return false;
        }
    }

    /**
     * 
     * @author Nicola Lagomarsini, GeoSolutions
     * 
     */
    static class LayerMap {

        private final ReentrantReadWriteLock lock;

        private final WriteLock writeLock;

        private final ReadLock readLock;

        private final ConcurrentHashMap<String, Set<String>> layerMap = new ConcurrentHashMap<String, Set<String>>();

        public LayerMap() {
            lock = new ReentrantReadWriteLock(true);
            writeLock = lock.writeLock();
            readLock = lock.readLock();
        }

        public void putTile(String layer, String id) {
            readLock.lock();
            Set<String> tileKeys = layerMap.get(layer);
            if (tileKeys == null) {
                readLock.unlock();
                writeLock.lock();
                try {
                    if (tileKeys == null) {
                        // If no key is present then a new KeySet is created and then added to the multimap
                        tileKeys = new ConcurrentSkipListSet<String>();
                        layerMap.put(layer, tileKeys);
                    }
                    readLock.lock();
                } finally {
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

        public void removeTile(String layer, String id) {
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
                                removeLayer(layer);
                            }
                        } finally {
                            writeLock.unlock();
                        }
                        readLock.lock();
                    }
                }
            } finally {
                readLock.unlock();
            }
        }

        public Set<String> removeLayer(String layer) {
            writeLock.lock();
            try {
                Set<String> layers = layerMap.get(layer);
                layerMap.remove(layer);
                return layers;
            } finally {
                writeLock.unlock();
            }
        }
    }
}
