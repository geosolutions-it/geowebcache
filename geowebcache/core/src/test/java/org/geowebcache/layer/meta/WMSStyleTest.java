package org.geowebcache.layer.meta;

import junit.framework.TestCase;

/**
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class WMSStyleTest extends TestCase {
    
    public void testBasic(){
        final WMSStyle style1= new WMSStyle();
        style1.setDefaultStyle(true);
        style1.setDescription("aStyle");
        style1.setIdentifier("aStyle");
        style1.setLegendMimeType("image/png");
        style1.setLegendURI("http://some.legend1.png");     
        
        assertTrue(style1.isDefaultStyle());
        assertEquals("aStyle", style1.getDescription());
        assertEquals("aStyle", style1.getIdentifier());
        assertEquals("image/png", style1.getLegendMimeType());
        assertEquals("http://some.legend1.png", style1.getLegendURI());
    }
}
