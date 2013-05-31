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
 * @author Arne Kepp, OpenGeo, Copyright 2009
 */
package org.geowebcache.grid;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.geowebcache.GeoWebCacheException;
/**
 * 
 * @author Simone Giannecchini, GeoSOlutions SAS
 * TODO checks on authorities
 */
public class SRS implements Comparable<SRS> {

    private static Map<Integer, SRS> list = new ConcurrentHashMap<Integer, SRS>();

    private static final SRS EPSG4326 = new SRS(4326);
    
    private static final SRS CRS84 = new SRS(84,null,"CRS");

    /**
     * The EPSG says EPSG:3857 is the identifier for web mercator. ArcGIS 10 says either of
     * EPSG:102113 or EPSG:102100 identifies web mercator. The "community" first defined it as
     * EPSG:900913.
     */
    private static final SRS EPSG3857 = new SRS(3857, new ArrayList<Integer>(asList(900913, 102113,
            102100)));

    /**
     * The EPSG says EPSG:3857 is the identifier for web mercator. ArcGIS 10 says either of
     * EPSG:102113 or EPSG:102100 identifies web mercator. The "community" first defined it as
     * EPSG:900913.
     */
    private static final SRS EPSG900913 = new SRS(900913, new ArrayList<Integer>(asList(3857,
            102113, 102100)));

    private int number;
    
    transient private String authority;

    private transient List<Integer> aliases;

    private SRS() {
        // default constructor for XStream
    }
    
    private SRS(int epsgNumber) {
        this(epsgNumber, null);
    }

    private SRS(int epsgNumber, List<Integer> aliases) {
        this(epsgNumber, aliases, "EPSG");
    }
    
    private SRS(int epsgNumber, List<Integer> aliases,String authority) {
        this.number = epsgNumber;
        this.aliases = aliases;
        this.authority=authority;
        readResolve();
    }

    // called by XStream for custom initialization
    private SRS readResolve() {
        if (!list.containsKey(Integer.valueOf(number))) {
            list.put(number, this);
        }
        return this;
    }

    /**
     * Returns an SRS object for the given code.
     * <p>
     * If an SRS for this code already exists, it's returned. Otherwise a registered SRS is looked
     * up that has an alias defined for the given code, and if found the alias is returned. If no
     * SRS is registered nor an alias is found, a new SRS for this code is registered and returned.
     * 
     * <p>
     * As a peculiar case 84 mas to CRS:84.
     * 
     * @param srsCode
     * @return
     * TODO threadsafety!!!
     */
    public static SRS getSRS(final int srsCode) {
        final Integer code = Integer.valueOf(srsCode);
        final SRS existing = list.get(code);

        if (existing != null) {
            return existing;
        }
        for (SRS candidate : new ArrayList<SRS>(list.values())) {
            if (candidate.aliases != null && candidate.aliases.contains(Integer.valueOf(code))) {
                list.put(code, candidate);
                return candidate;
            }
        }

        return new SRS(srsCode);
    }
    
    /**
     * Returns an SRS object for the given code. The code may contain an authority which is going to be checked internally!
     * <p>
     * If an SRS for this code already exists, it's returned. Otherwise a registered SRS is looked
     * up that has an alias defined for the given code, and if found the alias is returned. If no
     * SRS is registered nor an alias is found, a new SRS for this code is registered and returned.
     * 
     * <p>
     * As a peculiar case 84 mas to CRS:84.
     * 
     * @param srsCode
     * @return
     * TODO threadsafety!!!
     */
    public static SRS getSRS(String srsString) throws GeoWebCacheException {
        if(srsString==null){
            throw new NullPointerException("Unable to parse null CRS");
        }
        if(srsString.length()<=0){
            throw new NullPointerException("Unable to parse empty CRS");
        }        
        final int index=srsString.indexOf(":");
        String authority=null;
        if (index>=0) {
            authority=srsString.substring(0,index);
            srsString = srsString.substring(index+1, srsString.length());
        }
        
        int code = Integer.parseInt(srsString);
        SRS tmp = getSRS(code);
        if(tmp==null){
            throw new IllegalStateException("Unable to parse srs "+srsString);
        }
        if(tmp.getAuthority()!=null&&!tmp.getAuthority().equalsIgnoreCase(authority)){
            throw new IllegalStateException("Unable to parse srs "+srsString+"; Wrong authority retrieved!");
        }
        return tmp;
    }

    /**
     * Two SRS are equal if they have the same code or any of them have the other one as an alias.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SRS)) {
            return false;
        }
        boolean equivalent = false;
        SRS other = (SRS) obj;
        if (other.number == this.number) {
            equivalent = true;
        } else if (this.aliases != null && other.aliases != null) {
            equivalent = this.aliases.contains(other.number) || other.aliases.contains(this.number);
        }
        return equivalent;
    }

    /**
     * @deprecated just use {@link #equals}
     */
    public boolean equalsIncludingAlias(Object o) {
        return equals(o);
    }

    public int getNumber() {
        return number;
    }

    @Override
    public int hashCode() {
        return number;
    }

    public String toString() {
        return authority+":" + Integer.toString(number);
    }

    public static SRS getEPSG4326() {
        return EPSG4326;
    }

    public static SRS getEPSG3857() {
        return EPSG3857;
    }
    
    public static SRS getCRS84() {
        return CRS84;
    }

    public static SRS getEPSG900913() {
        return EPSG900913;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(SRS other) {
        return number - other.number;
    }

    /**
     * @return the authority
     */
    public String getAuthority() {
        return authority;
    }
}
