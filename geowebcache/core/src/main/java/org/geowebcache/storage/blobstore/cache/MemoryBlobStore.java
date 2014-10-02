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

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.BlobStoreListener;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileObject;
import org.geowebcache.storage.TileRange;

public class MemoryBlobStore implements BlobStore {

    private final static Log LOG = LogFactory.getLog(MemoryBlobStore.class);

    /** {@link BlobStore} to use when no element is found */
    private BlobStore store;

    /** {@link CacheProvider} object to use for caching */
    private CacheProvider cache;

    private final ExecutorService executorService;
    
    private final ReentrantReadWriteLock lock;

    private final WriteLock writeLock;

    private final ReadLock readLock;

    public MemoryBlobStore() {
        this.executorService = Executors.newFixedThreadPool(1);
        lock = new ReentrantReadWriteLock(true);
        writeLock = lock.writeLock();
        readLock = lock.readLock();
        // Initialization of the cache and store. Must be overridden
        setStore(new NullBlobStore());
        GuavaCacheProvider startingCache = new GuavaCacheProvider();
        startingCache.setConfiguration(new CacheConfiguration());
        setCache(startingCache);
    }

    @Override
    public boolean delete(String layerName) throws StorageException {
        readLock.lock();
        try{
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
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean deleteByGridsetId(String layerName, String gridSetId) throws StorageException {
        readLock.lock();
        try{
            // Remove the layer from the cache
            cache.removeLayer(layerName);
            // Remove selected gridsets
            executorService.submit(new BlobStoreTask(store, BlobStoreAction.DELETE_GRIDSET, layerName,
                    gridSetId));
            return true;
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean delete(TileObject obj) throws StorageException {
        readLock.lock();
        try{
            // Remove from cache
            cache.removeTileObj(obj);
            // Remove selected TileObject
            executorService.submit(new BlobStoreTask(store, BlobStoreAction.DELETE_SINGLE, obj));
            return true;
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean delete(TileRange obj) throws StorageException {
        readLock.lock();
        try{
            // flush the cache
            cache.clearCache();
            // Remove selected TileRange
            executorService.submit(new BlobStoreTask(store, BlobStoreAction.DELETE_RANGE, obj));
            return true;
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean get(TileObject obj) throws StorageException {
        readLock.lock();
        try{
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
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void put(TileObject obj) throws StorageException {
        readLock.lock();
        try{
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
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void clear() throws StorageException {
        readLock.lock();
        try{
            // flush the cache
            cache.clearCache();
            // Remove all the files
            executorService.submit(new BlobStoreTask(store, BlobStoreAction.CLEAR, ""));
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void destroy() {
        writeLock.lock();
        try{
            // flush the cache
            cache.resetCache();
            // Remove all the files
            Future<Boolean> future = executorService.submit(new BlobStoreTask(store, BlobStoreAction.DESTROY, ""));
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
            executorService.shutdownNow();
        }finally{
            writeLock.unlock();
        }
    }

    @Override
    public void addListener(BlobStoreListener listener) {
        readLock.lock();
        try{
            store.addListener(listener);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean removeListener(BlobStoreListener listener) {
        readLock.lock();
        try{
            return store.removeListener(listener);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public boolean rename(String oldLayerName, String newLayerName) throws StorageException {
        readLock.lock();
        try{
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
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public String getLayerMetadata(String layerName, String key) {
        readLock.lock();
        try{
            return store.getLayerMetadata(layerName, key);
        }finally{
            readLock.unlock();
        }
    }

    @Override
    public void putLayerMetadata(String layerName, String key, String value) {
        readLock.lock();
        try{
            store.putLayerMetadata(layerName, key, value);
        }finally{
            readLock.unlock();
        }
    }

    public CacheStatistics getCacheStatistics() {
        readLock.lock();
        try{
            return cache.getStats();
        }finally{
            readLock.unlock();
        }
    }

    public void setStore(BlobStore store) {
        writeLock.lock();
        try{
            if(store == null){
                throw new IllegalArgumentException("Input BlobStore cannot be null");
            }
            this.store = store;
        }finally{
            writeLock.unlock();
        }
    }
    
    public BlobStore getStore() {
        readLock.lock();
        try{
            return store;
        }finally{
            readLock.unlock();
        }
    }

    public void setCache(CacheProvider cache) {
        writeLock.lock();
        try{
            if(cache == null){
                throw new IllegalArgumentException("Input BlobStore cannot be null");
            }
            this.cache = cache;
        }finally{
            writeLock.unlock();
        }
    }

    private TileObject getByteResourceTile(TileObject obj) throws StorageException {

        Resource blob = obj.getBlob();
        final Resource finalBlob; 
        if (obj.getBlob() instanceof ByteArrayResource) {
            ByteArrayResource byteArrayResource = (ByteArrayResource) obj.getBlob();
            byte[] contents = byteArrayResource.getContents();
            byte[] copy = new byte[contents.length];
            System.arraycopy(contents, 0, copy, 0, contents.length);
            finalBlob = new ByteArrayResource(copy);        
        }else{
            final ByteArrayOutputStream bOut= new ByteArrayOutputStream();
            WritableByteChannel wChannel = Channels.newChannel(bOut);
            try {
                blob.transferTo(wChannel);
            } catch (IOException e) {
               throw new StorageException(e.getLocalizedMessage(), e);
                
            }
            finalBlob = new ByteArrayResource(bOut.toByteArray());
        }

        TileObject cached= TileObject.createCompleteTileObject(obj.getLayerName(), obj.getXYZ(),
                obj.getGridSetId(), obj.getBlobFormat(), obj.getParameters(), finalBlob);
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
