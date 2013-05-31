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
package org.geowebcache.layer.meta;

import java.io.Serializable;

/**
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class WMSStyle implements Serializable {
    
    /** serialVersionUID */
    private static final long serialVersionUID = -6281324211561272283L;

    private String identifier;
    
    private String description;
    
    private String legendURI;
    
    private String legendMimeType;
    
    private boolean defaultStyle;

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the legendURI
     */
    public String getLegendURI() {
        return legendURI;
    }

    /**
     * @return the legendMimeType
     */
    public String getLegendMimeType() {
        return legendMimeType;
    }

    /**
     * @param identifier the identifier to set
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param legendURI the legendURI to set
     */
    public void setLegendURI(String legendURI) {
        this.legendURI = legendURI;
    }

    /**
     * @param legendMimeType the legendMimeType to set
     */
    public void setLegendMimeType(String legendMimeType) {
        this.legendMimeType = legendMimeType;
    }

    /**
     * @return the defaultStyle
     */
    public boolean isDefaultStyle() {
        return defaultStyle;
    }

    /**
     * @param defaultStyle the defaultStyle to set
     */
    public void setDefaultStyle(boolean defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WMSStyle [");
        if (identifier != null) {
            builder.append("identifier=");
            builder.append(identifier);
            builder.append(", ");
        }
        if (description != null) {
            builder.append("description=");
            builder.append(description);
            builder.append(", ");
        }
        if (legendURI != null) {
            builder.append("legendURI=");
            builder.append(legendURI);
            builder.append(", ");
        }
        if (legendMimeType != null) {
            builder.append("legendMimeType=");
            builder.append(legendMimeType);
            builder.append(", ");
        }
        builder.append("defaultStyle=");
        builder.append(defaultStyle);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (defaultStyle ? 1231 : 1237);
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((legendMimeType == null) ? 0 : legendMimeType.hashCode());
        result = prime * result + ((legendURI == null) ? 0 : legendURI.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof WMSStyle)) {
            return false;
        }
        WMSStyle other = (WMSStyle) obj;
        if (defaultStyle != other.defaultStyle) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (identifier == null) {
            if (other.identifier != null) {
                return false;
            }
        } else if (!identifier.equals(other.identifier)) {
            return false;
        }
        if (legendMimeType == null) {
            if (other.legendMimeType != null) {
                return false;
            }
        } else if (!legendMimeType.equals(other.legendMimeType)) {
            return false;
        }
        if (legendURI == null) {
            if (other.legendURI != null) {
                return false;
            }
        } else if (!legendURI.equals(other.legendURI)) {
            return false;
        }
        return true;
    }
}
