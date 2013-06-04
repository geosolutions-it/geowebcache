/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geowebcache.grid;

import org.geowebcache.GeoWebCacheException;

import junit.framework.TestCase;

/**
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class SRSTest extends TestCase {
    public void testBase() throws GeoWebCacheException{
        
        // 4326
        SRS srs = SRS.getSRS(4326);
        assertEquals(SRS.getEPSG4326(), srs);
        assertEquals("EPSG", srs.getAuthority());
        
        // 4326
        srs = SRS.getSRS(3857);
        assertEquals(SRS.getEPSG3857(), srs);
        assertEquals("EPSG", srs.getAuthority());
        
        // 900913
        srs = SRS.getSRS(900913);
        assertEquals(SRS.getEPSG900913(), srs);
        assertNotNull(srs.getAuthority());
        assertEquals("EPSG", srs.getAuthority());
        
        // CRS:84
        srs = SRS.getSRS(84);
        assertEquals(SRS.getCRS84(), srs);
        srs = SRS.getSRS("CRS:84");
        assertEquals(SRS.getCRS84(), srs);
        try{
            srs = SRS.getSRS("EPSG:84");
            assertTrue(false);
        }catch (Exception e) {
            assertTrue(true);
        }
        
        // EPSG 32632
        srs = SRS.getSRS(32632);
        assertEquals("EPSG", srs.getAuthority());
        assertEquals(32632, srs.getNumber());

    }
}
