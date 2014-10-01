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
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class contains the configuration for the cache object to use.
 * 
 * @author Nicola Lagomarsini Geosolutions
 */
public class CacheConfiguration implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 3875121032331372267L;

    /** Default value for the Cache memory limit */
    public static final long DEFAULT_MEMORY_LIMIT = 16 * 1024 * 1024; // 16Mb

    /** Default value for the cache concurrency level */
    public static final int DEFAULT_CONCURRENCY_LEVEL = 4;

    /** Parameter associated to the Cache memory limit */
    private long hardMemoryLimit = DEFAULT_MEMORY_LIMIT;

    /** Parameter associated to Layers to skip when caching */
    private Set<String> layers = new ConcurrentSkipListSet<String>();

    /** Parameter associated to the Cache eviction policy */
    private String policy = "";

    /** Parameter associated to the Cache concurrency level */
    private int concurrencyLevel = DEFAULT_CONCURRENCY_LEVEL;

    public CacheConfiguration() {

    }

    public CacheConfiguration(CacheConfiguration config) {
        this.concurrencyLevel = config.getConcurrencyLevel();
        this.hardMemoryLimit = config.getHardMemoryLimit();
        this.policy = new String(config.getPolicy());

        if (config.getLayers() != null) {
            this.layers = new ConcurrentSkipListSet<String>(config.getLayers());
        }

    }

    /**
     * @return the current cache memory limit
     */
    public long getHardMemoryLimit() {
        return hardMemoryLimit;
    }

    /**
     * Sets the cache memory limit
     * 
     * @param hardMemoryLimit
     */
    public void setHardMemoryLimit(long hardMemoryLimit) {
        this.hardMemoryLimit = hardMemoryLimit;
    }

    /**
     * @return the actual collection of layers to skip when caching
     */
    public Set<String> getLayers() {
        return layers;
    }

    /**
     * Adds new Layers to avoid caching
     * 
     * @param layers
     */
    public synchronized void setLayers(Set<String> layers) {
        if (this.layers == null) {
            this.layers = new ConcurrentSkipListSet<>();
        }
        if (layers != null) {
            this.layers.addAll(layers);
        }
    }

    /**
     * @return The cache eviction policy
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * Sets a new cache eviction policy
     * 
     * @param policy
     */
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    /**
     * @return the cache concurrency level
     */
    public int getConcurrencyLevel() {
        return concurrencyLevel;
    }

    /**
     * Sets the cache concurrency level
     * 
     * @param concurrencyLevel
     */
    public void setConcurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof CacheConfiguration)) {
            return false;
        }
        CacheConfiguration config = (CacheConfiguration) obj;
        if (this.concurrencyLevel != config.concurrencyLevel) {
            return false;
        } else if (!this.policy.equals(config.policy)) {
            return false;
        } else if (this.hardMemoryLimit != config.hardMemoryLimit) {
            return false;
        } else if ((layers == null || layers.isEmpty())
                && !(config.layers == null || config.layers.isEmpty())) {
            return false;
        }

        return true;
    }
}
