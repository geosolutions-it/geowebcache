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
package org.geowebcache.service;

import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;

public class OWSException extends Exception {
    private static final long serialVersionUID = -8024005353689857211L;

    int httpCode;

    String exceptionCode;

    String locator;

    String exceptionText;
    
    String language;

    /**
     * Full constructor for an {@link OWSException}.
     * 
     * @param httpCode
     * @param exceptionCode
     * @param locator
     * @param exceptionText
     * @param language
     */
    public OWSException(int httpCode, String exceptionCode, String locator, String exceptionText,
            String language) {

        this.httpCode = httpCode;
        this.exceptionCode = exceptionCode;
        this.locator = locator;
        this.exceptionText = exceptionText;
        this.language=language;
    }

    public OWSException(int httpCode, String exceptionCode, String locator, String exceptionText) {
        this(httpCode, exceptionCode, locator, exceptionText,null);
    }
    
    public int getResponseCode() {
        return httpCode;
    }
    
    public String getContentType() {
        return "text/xml";
    }
    
    public Resource getResponse() {
        return new ByteArrayResource(this.toString().getBytes());
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        str.append("<ExceptionReport version=\"1.1.0\" xmlns=\"http://www.opengis.net/ows/1.1\"\n");
        str.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        str.append("  xsi:schemaLocation=\"http://www.opengis.net/ows/1.1").append(" http://geowebcache.org/schema/ows/1.1.0/owsExceptionReport.xsd\"");
        if(language!=null){
            str.append(" xml:lang=\"").append(language).append("\"");
        }
        str.append(">\n");
        str.append("  <Exception exceptionCode=\"").append(exceptionCode).append( "\" locator=\"" ).append( locator ).append( "\">\n");
        str.append("    <ExceptionText>" ).append( exceptionText ).append( "</ExceptionText>\n");
        str.append("  </Exception>\n");
        str.append("</ExceptionReport>\n");

        return str.toString();
    }

}

