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
 * 
 */
package org.geowebcache.rest.statistics;

import org.geowebcache.storage.BlobStore;
import org.geowebcache.storage.blobstore.cache.CacheStatistics;
import org.geowebcache.storage.blobstore.cache.MemoryBlobStore;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;

public class MemoryCacheStatsResource extends Resource {

    private BlobStore store;

    public void setBlobStore(BlobStore store) {
        this.store = store;
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    @Override
    public boolean allowPut() {
        return false;
    }

    @Override
    public boolean allowPost() {
        return false;
    }

    @Override
    public boolean allowDelete() {
        return false;
    }

    @Override
    public void handleGet() {
        final Request request = getRequest();
        final Response response = getResponse();
        final String formatExtension = (String) request.getAttributes().get("extension");

        // Getting the store statistics if it is a MemoryCacheBlobStore
        Representation representation;
        if (store != null && store instanceof MemoryBlobStore) {

            MemoryBlobStore memoryStore = (MemoryBlobStore) store;
            CacheStatistics stats = memoryStore.getCacheStatistics();
            CacheStatistics statistics = new CacheStatistics(stats);
            if ("json".equals(formatExtension)) {
                try {
                    representation = getJsonRepresentation(statistics);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            } else if ("xml".equals(formatExtension)) {
                representation = getXmlRepresentation(statistics);
            } else {
                response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
                        "Unknown or missing format extension : " + formatExtension);
                return;
            }

            response.setEntity(representation);
            response.setStatus(Status.SUCCESS_OK);
        } else {
            response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,
                    "No statistics available for the current BlobStore: " + store.getClass());
        }

    }

    private JsonRepresentation getJsonRepresentation(CacheStatistics stats) throws JSONException {
        XStream xs = getConfiguredXStream(new XStream(new JsonHierarchicalStreamDriver()));
        JSONObject obj = new JSONObject(xs.toXML(stats));
        JsonRepresentation rep = new JsonRepresentation(obj);
        return rep;
    }

    private Representation getXmlRepresentation(CacheStatistics stats) {
        XStream xStream = getConfiguredXStream(new XStream());
        String xml = xStream.toXML(stats);
        return new StringRepresentation(xml, MediaType.TEXT_XML);
    }

    public static XStream getConfiguredXStream(XStream xs) {
        xs.setMode(XStream.NO_REFERENCES);

        xs.alias("gwcBlobStoreStatistics", CacheStatistics.class);
        return xs;
    }
}
