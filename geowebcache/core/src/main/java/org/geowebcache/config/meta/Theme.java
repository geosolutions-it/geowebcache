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
package org.geowebcache.config.meta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Theme info for INSPIRE Compliancy
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class Theme implements Serializable {
    
    /** serialVersionUID */
    private static final long serialVersionUID = -559482994114428019L;

    private String title;
    
    private String identifier;
    
    private List<String> layerRefs= new ArrayList<String>();

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return the layerRefs
     */
    public List<String> getLayerRefs() {
        return layerRefs;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @param layerRefs the layerRefs to set
     */
    public void setLayerRefs(List<String> layerRefs) {
        if(layerRefs==null){
            throw new NullPointerException();
        }
        this.layerRefs= new ArrayList<String>(layerRefs);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,ToStringStyle.MULTI_LINE_STYLE);
    }
}
