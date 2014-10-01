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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.FileResource;
import org.geowebcache.io.Resource;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.BlobStoreListener;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;

public class MemoryBlobStore implements BlobStore {

    public static Log LOG = LogFactory.getLog(MemoryBlobStore.class);

    /** {@link BlobStore} to use when no element is found */
    private BlobStore store;

    /** {@link CacheProvider} object to use for caching */
    private CacheProvider cache;

    private final ExecutorService executorService;

    public MemoryBlobStore() throws StorageException {
        this.executorService = Executors.newFixedThreadPool(1);
    }

    @Override
    public boolean delete(String layerName) throws StorageException {
        // Remove from cache
        cache.removeLayer(layerName);
        // Remove the layer. Wait other scheduled tasks
        Future<Boolean> future = executorService.submit(new BlobStoreTask(store,
                BlobStoreAction.DELETE_LAYER, layerName));
        // Variable containing the execution result
        boolean executed = false;
        try {
            executed = future.get();
        } catch (InterruptedException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        } catch (ExecutionException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        }
        return executed;
    }

    @Override
    public boolean deleteByGridsetId(String layerName, String gridSetId) throws StorageException {
        // Remove the layer from the cache
        cache.removeLayer(layerName);
        // Remove selected gridsets
        executorService.submit(new BlobStoreTask(store, BlobStoreAction.DELETE_GRIDSET, layerName,
                gridSetId));
        return true;
    }

    @Override
    public boolean delete(TileObject obj) throws StorageException {
        // Remove from cache
        cache.removeTileObj(obj);
        // Remove selected TileObject
        executorService.submit(new BlobStoreTask(store, BlobStoreAction.DELETE_SINGLE, obj));
        return true;
    }

    @Override
    public boolean delete(TileRange obj) throws StorageException {
        // flush the cache
        cache.clearCache();
        // Remove selected TileRange
        executorService.submit(new BlobStoreTask(store, BlobStoreAction.DELETE_RANGE, obj));
        return true;
    }

    @Override
    public boolean get(TileObject obj) throws StorageException {
        TileObject cached = cache.getTileObj(obj);
        boolean found = false;
        if (cached == null) {
            // Try if it can be found in the system. Wait other scheduled tasks
            Future<Boolean> future = executorService.submit(new BlobStoreTask(store,
                    BlobStoreAction.GET, obj));
            try {
                found = future.get();
            } catch (InterruptedException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
            } catch (ExecutionException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
            }
            // If the file has been found, it is inserted in cache
            if (found) {
                cached = getByteResourceTile(obj);
                // Put the file in Cache
                cache.putTileObj(cached);
            }
        } else {
            found = true;
        }
        if (found) {
            Resource resource = cached.getBlob();
            obj.setBlob(resource);
            obj.setCreated(resource.getLastModified());// TODO???
            obj.setBlobSize((int) resource.getSize());
        }

        return found;
    }

    @Override
    public void put(TileObject obj) throws StorageException {
        TileObject cached = getByteResourceTile(obj);
        cache.putTileObj(cached);
        // Add selected TileObject. Wait other scheduled tasks
        Future<Boolean> future = executorService.submit(new BlobStoreTask(store,
                BlobStoreAction.PUT, obj));
        // Variable containing the execution result
        try {
            future.get();
        } catch (InterruptedException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        } catch (ExecutionException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void clear() throws StorageException {
        // flush the cache
        cache.clearCache();
        // Remove all the files
        executorService.submit(new BlobStoreTask(store, BlobStoreAction.CLEAR, ""));
    }

    @Override
    public void destroy() {
        // flush the cache
        cache.clearCache();
        // Remove all the files
        executorService.submit(new BlobStoreTask(store, BlobStoreAction.DESTROY, ""));
        executorService.shutdownNow();
    }

    @Override
    public void addListener(BlobStoreListener listener) {
        store.addListener(listener);
    }

    @Override
    public boolean removeListener(BlobStoreListener listener) {
        return store.removeListener(listener);
    }

    @Override
    public boolean rename(String oldLayerName, String newLayerName) throws StorageException {
        // flush the cache
        cache.clearCache();
        // Rename the layer. Wait other scheduled tasks
        Future<Boolean> future = executorService.submit(new BlobStoreTask(store,
                BlobStoreAction.RENAME, oldLayerName, newLayerName));
        // Variable containing the execution result
        boolean executed = false;
        try {
            executed = future.get();
        } catch (InterruptedException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        } catch (ExecutionException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
        }
        return executed;
    }

    @Override
    public String getLayerMetadata(String layerName, String key) {
        return store.getLayerMetadata(layerName, key);
    }

    @Override
    public void putLayerMetadata(String layerName, String key, String value) {
        store.putLayerMetadata(layerName, key, value);
    }

    public CacheStatistics getCacheStatistics() {
        return cache.getStats();
    }

    public void setStore(BlobStore store) {
        this.store = store;
    }
    
    public BlobStore getStore() {
        return store;
    }

    public void setCache(CacheProvider cache) {
        this.cache = cache;
    }

    private TileObject getByteResourceTile(TileObject obj) throws StorageException {
        TileObject cached;
        if (obj.getBlob() instanceof FileResource) {
            File fileResource = ((FileResource) obj.getBlob()).getFile();
            Path path = Paths.get(fileResource.toURI());
            Resource blob = null;
            try {
                blob = new ByteArrayResource(Files.readAllBytes(path));
            } catch (IOException e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
                throw new StorageException(e.getMessage(), e);
            }
            cached = TileObject.createCompleteTileObject(obj.getLayerName(), obj.getXYZ(),
                    obj.getGridSetId(), obj.getBlobFormat(), obj.getParameters(), blob);
        } else {
            ByteArrayResource byteArrayResource = (ByteArrayResource) obj.getBlob();
            byte[] contents = byteArrayResource.getContents();
            
            Resource blob = new ByteArrayResource(Arrays.copyOf(contents, contents.length));
            cached = TileObject.createCompleteTileObject(obj.getLayerName(), obj.getXYZ(),
                    obj.getGridSetId(), obj.getBlobFormat(), obj.getParameters(), blob);
        }
        return cached;
    }

    static class BlobStoreTask implements Callable<Boolean> {

        private BlobStore store;

        private Object[] objs;

        private BlobStoreAction action;

        public BlobStoreTask(BlobStore store, BlobStoreAction action, Object... objs) {
            this.objs = objs;
            this.store = store;
            this.action = action;
        }

        @Override
        public Boolean call() throws Exception {
            boolean result = false;
            try {
                result = action.executeOperation(store, objs);
            } catch (StorageException s) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(s.getMessage(), s);
                }
            }
            return result;
        }

    }

    public enum BlobStoreAction {
        PUT {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                if (objs == null || objs.length < 1 || !(objs[0] instanceof TileObject)) {
                    return false;
                }
                store.put((TileObject) objs[0]);
                return true;
            }
        },
        GET {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                if (objs == null || objs.length < 1 || !(objs[0] instanceof TileObject)) {
                    return false;
                }
                return store.get((TileObject) objs[0]);
            }
        },
        DELETE_SINGLE {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                if (objs == null || objs.length < 1 || !(objs[0] instanceof TileObject)) {
                    return false;
                }
                return store.delete((TileObject) objs[0]);
            }
        },
        DELETE_RANGE {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                if (objs == null || objs.length < 1 || !(objs[0] instanceof TileRange)) {
                    return false;
                }
                return store.delete((TileRange) objs[0]);
            }
        },
        DELETE_GRIDSET {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                if (objs == null || objs.length < 2 || !(objs[0] instanceof String)
                        || !(objs[1] instanceof String)) {
                    return false;
                }
                return store.deleteByGridsetId((String) objs[0], (String) objs[1]);
            }
        },
        DELETE_LAYER {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                if (objs == null || objs.length < 2 || !(objs[0] instanceof String)) {
                    return false;
                }
                return store.delete((String) objs[0]);
            }
        },
        CLEAR {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                store.clear();
                return true;
            }
        },
        DESTROY {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                store.destroy();
                return true;
            }
        },
        RENAME {
            @Override
            public boolean executeOperation(BlobStore store, Object... objs)
                    throws StorageException {
                if (objs == null || objs.length < 2 || !(objs[0] instanceof String)
                        || !(objs[1] instanceof String)) {
                    return false;
                }
                return store.rename((String) objs[0], (String) objs[1]);
            }
        };

        public abstract boolean executeOperation(BlobStore store, Object... objs)
                throws StorageException;
    }
}
