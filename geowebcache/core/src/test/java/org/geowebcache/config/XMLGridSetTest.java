package org.geowebcache.config;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.SRS;
import org.w3c.dom.Document;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomReader;

/**
 * @author Simone Gianecchini, GeoSolutions SAS
 * 
 */
public class XMLGridSetTest extends TestCase {

    public void testCustomGridSet() throws Exception {
        
        // get file
        final InputStream xmlFile = this.getClass().getResourceAsStream("customgridset.xml");
        assertNotNull(xmlFile);
        
        // parse into dom
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document topNode = docBuilder.parse(xmlFile);
        assertNotNull(topNode);

        // unmarshal
        XStream xs = XMLConfiguration.getConfiguredXStream(new XStream(), null);
        Object o = xs.unmarshal(new DomReader(topNode));
        assertNotNull(o);
        assertTrue(o instanceof XMLGridSet);
        
        // check gridset
        final XMLGridSet gridSet= (XMLGridSet) o;
        
        // srs
        final SRS srs = gridSet.getSrs();
        assertNotNull(srs);
        assertNotNull(srs.getAuthority());
        assertEquals("EPSG", srs.getAuthority());
        assertEquals(900913, srs.getNumber());
        
        // bbox
        final BoundingBox bbox = gridSet.getExtent();
        assertNotNull(bbox);
        assertEquals(1269600.0,bbox.getMaxX());
        assertEquals(5442200.0,bbox.getMaxY());
        assertEquals(1233700.0,bbox.getMinX());
        assertEquals(5418400.0,bbox.getMinY());
        
        // resolution
        final double[] res = gridSet.getResolutions();
        assertNotNull(res);
        assertEquals(21, res.length);
        assertEquals(78271.516953125,res[0]);
        assertEquals(0.07464553542435169,res[20]);        
    }
}
