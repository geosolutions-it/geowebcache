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

/**
 * @author Simone Giannecchini, GeoSolutions
 *
 */
public class INSPIREAdditionalInformation implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = -7011315111034763251L;
    
    private String defaultLanguage;
    
    private List<String> additionalLanguages= new ArrayList<String>();
    
    private String linkViewServiceLink;
    
    private List<Theme> themes= new ArrayList<Theme>();
    
    /**
     * @return the themes
     */
    public List<Theme> getThemes() {
        return themes;
    }

    /**
     * @param themes the themes to set
     */
    public void setThemes(List<Theme> themes) {
        this.themes=new ArrayList<Theme>(themes);
    }

    /**
     * @return the defaultLanguage
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * @return the additionalLanguages
     */
    public List<String> getAdditionalLanguages() {
        return additionalLanguages;
    }

    /**
     * @return the linkViewServiceLink
     */
    public String getLinkViewServiceLink() {
        return linkViewServiceLink;
    }

    /**
     * @param defaultLanguage the defaultLanguage to set
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    /**
     * @param additionalLanguages the additionalLanguages to set
     */
    public void setAdditionalLanguages(List<String> additionalLanguages) {
        this.additionalLanguages= new ArrayList<String>(additionalLanguages);
    }

    /**
     * @param linkViewServiceLink the linkViewServiceLink to set
     */
    public void setLinkViewServiceLink(String linkViewServiceLink) {
        this.linkViewServiceLink = linkViewServiceLink;
    }

}
