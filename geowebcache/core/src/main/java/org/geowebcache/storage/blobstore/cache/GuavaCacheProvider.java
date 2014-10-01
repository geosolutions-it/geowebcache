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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.storage.TileObject;

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

    /** {@link Log} object used for reporting informations */
    private final static Log LOG = LogFactory.getLog(GuavaCacheProvider.class);

    /** Separator char used for creating Cache keys */
    public final static String SEPARATOR = "_";
    
    public static class GuavaCacheStatistics extends CacheStatistics{

        public GuavaCacheStatistics(CacheStats stats) {
            this.setEvictionCount(stats.evictionCount());
            this.setHitCount(stats.hitCount());
            this.setMissCount(getMissCount());
        }
    
    }

    /** {@link CacheConfiguration} object used for creating the cache */
    private CacheConfiguration configuration;

    /** Cache object containing the various {@link TileObject}s */
    private Cache<String, TileObject> cache;

    private LayerMap multimap;

    private CacheStatistics cacheStatistics;

    private long maxMemory = 0;

    private int concurrency = 0;

    public GuavaCacheProvider() {
        cacheStatistics = new CacheStatistics();
    }

    private void initCache(int concurrency, long maxMemory) {

        if (this.concurrency == 0 && this.maxMemory == 0) {
            this.concurrency = concurrency;
            this.maxMemory = maxMemory;
        } else if (this.concurrency == concurrency && this.maxMemory == maxMemory) {
            return;
        }

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
        // Build the cache
        cache = newBuilder.build();

        // Created a new multimap
        multimap = new LayerMap();
    }

    @Override
    public CacheConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public synchronized void setConfiguration(CacheConfiguration configuration) {
        this.configuration = configuration;
        initCache(configuration.getConcurrencyLevel(), configuration.getHardMemoryLimit());
    }

    @Override
    public TileObject getTileObj(TileObject obj) {
        if (configuration.getLayers() != null && !configuration.getLayers().isEmpty()
                && configuration.getLayers().contains(obj.getLayerName())) {
            return null;
        }
        String id = generateTileKey(obj);
        return cache.getIfPresent(id);
    }

    @Override
    public void putTileObj(TileObject obj) {
        if (configuration.getLayers() != null && !configuration.getLayers().isEmpty()
                && configuration.getLayers().contains(obj.getLayerName())) {
            return;
        }
        String id = generateTileKey(obj);
        // TODO This operation is not atomic
        cache.put(id, obj);
        multimap.putTile(obj.getLayerName(), id);
    }

    @Override
    public void removeTileObj(TileObject obj) {
        if (configuration.getLayers() != null && !configuration.getLayers().isEmpty()
                && configuration.getLayers().contains(obj.getLayerName())) {
            return;
        }
        String id = generateTileKey(obj);
        cache.invalidate(id);
    }

    @Override
    public void removeLayer(String layername) {
        if (configuration.getLayers() != null && !configuration.getLayers().isEmpty()
                && configuration.getLayers().contains(layername)) {
            return;
        }
        Set<String> keys = multimap.getLayerIds(layername);
        if (keys != null) {
            cache.invalidateAll(keys);
        }
    }

    @Override
    public synchronized void clearCache() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    @Override
    public CacheStatistics getStats() {
        if (cache == null) {
            return cacheStatistics;
        }
        CacheStats stats = cache.stats();
        cacheStatistics.setEvictionCount(stats.evictionCount());
        cacheStatistics.setHitCount(stats.hitCount());
        cacheStatistics.setMissCount(stats.missCount());

        return cacheStatistics;
    }

    public static String generateTileKey(TileObject obj) {
        return obj.getLayerName() + SEPARATOR + obj.getGridSetId() + SEPARATOR
                + Arrays.toString(obj.getXYZ()) + SEPARATOR + obj.getBlobFormat();
    }

    /**
     * 
     * @author Nicola Lagomarsini, GeoSolutions
     *
     */
    static class LayerMap {

        private final ConcurrentHashMap<String, Set<String>> layerMap = new ConcurrentHashMap<String, Set<String>>();

        public LayerMap() {
            
        }

        public void putTile(String layer, String id) {
            Set<String> tileKeys = null;
            synchronized (layerMap) {
                // Check if the multimap contains the keys for the image
                tileKeys = layerMap.get(layer);
                if (tileKeys == null) {
                    // If no key is present then a new KeySet is created and then added to the multimap
                    tileKeys = new ConcurrentSkipListSet<String>();
                    layerMap.put(layer, tileKeys);
                }
            }
            // Finally the tile key is added.
            tileKeys.add(id);
        }

        public void removeTile(String layer, String id) {
            // KeySet associated to the image
            Set<String> tileKeys = layerMap.get(layer);
            if (tileKeys != null) {
                // Removal of the keys
                tileKeys.remove(id);
                // If the KeySet is empty then it is removed from the multimap
                if (tileKeys.isEmpty()) {
                    removeLayer(layer);
                }
            }
        }

        // TODO if layer is present
        // TODO return existing Set of tiles
        public void removeLayer(String layer) {
            layerMap.remove(layer);
        }

        public Set<String> getLayerIds(String layer) {
            return layerMap.get(layer);
        }
    }
}
