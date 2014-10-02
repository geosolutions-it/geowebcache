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

import java.io.Serializable;
/**
 * 
 * @author Nicola Lagomarsini, GeoSolutions
 *
 */
public class CacheStatistics implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -1049287017217353112L;

    private long hitCount = 0;

    private long missCount = 0;

    private long evictionCount = 0;
    
    private long totalCount = 0;
    
    private double hitRate = 0;
    
    private double missRate = 0;
    
    public CacheStatistics(){
    }

    // Copy Constructor
    public CacheStatistics(CacheStatistics stats) {
        this.setEvictionCount(stats.getEvictionCount());
        this.setHitCount(stats.getHitCount());
        this.setMissCount(stats.getMissCount());
        this.setTotalCount(stats.getRequestCount());
        this.setHitRate(stats.getHitRate());
        this.setMissRate(stats.getMissRate());
    }

    public long getHitCount() {
        return hitCount;
    }

    public void setHitCount(long hitCount) {
        this.hitCount = hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public void setMissCount(long missCount) {
        this.missCount = missCount;
    }

    public long getEvictionCount() {
        return evictionCount;
    }

    public void setEvictionCount(long evictionCount) {
        this.evictionCount = evictionCount;
    }

    public long getRequestCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public double getHitRate() {
        return hitRate;
    }

    public void setHitRate(double hitRate) {
        this.hitRate = hitRate;
    }

    public double getMissRate() {
        return missRate;
    }

    public void setMissRate(double missRate) {
        this.missRate = missRate;
    }
}
