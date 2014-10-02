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

import org.geowebcache.storage.TileObject;

public interface CacheProvider {

    /**
     * Returns the {@link TileObject} for the selected id
     */
    public TileObject getTileObj(TileObject obj);

    /**
     * Insert a {@link TileObject} in cache.
     * 
     * @param obj
     */
    public void putTileObj(TileObject obj);

    /**
     * Removes a {@link TileObject} from cache.
     * 
     * @param obj
     */
    public void removeTileObj(TileObject obj);

    /**
     * Removes all the {@link TileObject}s for the related layer from cache.
     * 
     * @param layername
     */
    public void removeLayer(String layername);

    /**
     * Removes all the cached {@link TileObject}s
     */
    public void clearCache();
    
    public void resetCache();

    /**
     * Returns a {@link CacheStatistics} object containing the current cache statistics.
     */
    public CacheStatistics getStats();

    /**
     * Sets the CacheConfiguration to use
     * 
     * @param configuration
     */
    void setConfiguration(CacheConfiguration configuration);
    
    public void addUncachedLayer(String layername);
    
    public void removeUncachedLayer(String layername);
    
    public boolean containsUncachedLayer(String layername);
}
