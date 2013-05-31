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
package org.geowebcache.layer.meta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LayerMetaInformation {
    String title;
    
    String description;
    
    List<String> keywords=new LinkedList<String>();
    
    List<ContactInformation> contacts = new LinkedList<ContactInformation>();
    
    List<String> metadataLinks= new ArrayList<String>();
    
    /**
     * @return the MetadataLinks
     */
    public List<String> getMetadataLinks() {
        return metadataLinks;
    }

    /**
     * @param metadataLinks the metadataLinks to set
     */
    public void setMetadataLinks(List<String> metadataLinks) {
        metadataLinks= new ArrayList<String>(metadataLinks);
    }

    LayerMetaInformation() {
        // default constructor for XStream
    }
    
    /**
     * @param title
     * @param description
     * @param keywords
     * @param contacts
     * @param metadataLinks
     */
    public LayerMetaInformation(String title, String description, List<String> keywords,
            List<ContactInformation> contacts, List<String> metadataLinks) {
        this.title = title;
        this.description = description;
        this.keywords = keywords;
        this.contacts = contacts;
        if(metadataLinks!=null){
            this.metadataLinks.addAll(metadataLinks);
        }
    }

    public String getTitle() {
        return title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public  List<String> getKeywords() {
        return keywords;
    }

    public List<ContactInformation> getContacts() {
        return contacts;
    }
}
