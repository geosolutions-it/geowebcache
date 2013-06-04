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
 * @author Simone Giannecchini, GeoSolutions 2013
 */
package org.geowebcache.layer.meta;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;

import junit.framework.TestCase;

/**
 * @author Simone Giannecchini, GeoSolutions 2013
 *
 */
public class LayerMetaInformationTest extends TestCase {

    /**
     * Test method for {@link org.geowebcache.layer.meta.LayerMetaInformation#getTitle()}.
     */
    public void testBasic() {
        final List<String> keys= new ArrayList<String>();
        keys.add("k1");
        keys.add("k2");
        keys.add("k3");        
        final List<String> metadataLinks= new ArrayList<String>();
        metadataLinks.add("http://www.geo-solutions.it");
        metadataLinks.add("http://www.geo-solutions.it");
        
        LayerMetaInformation layerMetaInfo= new LayerMetaInformation(
                "layer_title", 
                "description", 
                keys, 
                null,
                metadataLinks);
        
        // check the toString method
        String toString=layerMetaInfo.toString();
        assertNotNull(toString);
        
        assertNull(layerMetaInfo.getContacts());
        
        final List<String> metadataLinks2 = layerMetaInfo.getMetadataLinks();
        assertNotNull(metadataLinks2);
        assertEquals(2, metadataLinks2.size());
        assertTrue(EqualsBuilder.reflectionEquals(metadataLinks, metadataLinks2));
        
        final List<String> keywords = layerMetaInfo.getKeywords();
        assertNotNull(keywords);
        assertEquals(3, keywords.size());
        assertTrue(EqualsBuilder.reflectionEquals(keywords, keywords));
    }
}
