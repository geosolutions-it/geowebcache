package org.geowebcache.seed;

import java.util.Set;

import org.geowebcache.config.Configuration;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * A request to completely truncate a layer's cache.
 * 
 * @author Kevin Smith, OpenGeo
 */
@XStreamAlias("truncateLayer")
public class TruncateLayerRequest implements MassTruncateRequest {

    String layerName;

	@Override
	public boolean doTruncate(StorageBroker sb, Configuration config, Set<String> layers)
			throws StorageException {
		boolean truncated = sb.delete(layerName);
   	 if (!truncated) {
	    	 // did we hit a layer that has nothing on storage, or a layer that is not there?
	    	 if(!layers.contains(layerName)) {
	    		 throw new IllegalArgumentException("Could not find layer " + layerName);
	    	 }
   	 }
   	 return true;
	}

}
