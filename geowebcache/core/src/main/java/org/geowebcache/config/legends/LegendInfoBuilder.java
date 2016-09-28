/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Nuno Oliveira, GeoSolutions S.A.S., Copyright 2016
 */
package org.geowebcache.config.legends;

import com.google.common.base.Preconditions;
import org.geowebcache.util.ServletUtils;

/**
 * Builder for {@link LegendInfo} instances.
 */
public class LegendInfoBuilder {

    private String layerName;
    private String layerUrl;

    private Integer defaultWidth;
    private Integer defaultHeight;
    private String defaultFormat;

    private String styleName;
    private Integer width;
    private Integer height;
    private String format;
    private String url;
    private String completeUrl;

    public LegendInfoBuilder withLayerName(String layerName) {
        this.layerName = layerName;
        return this;
    }

    public LegendInfoBuilder withLayerUrl(String layerUrl) {
        this.layerUrl = layerUrl;
        return this;
    }

    public LegendInfoBuilder withDefaultWidth(Integer defaultWidth) {
        this.defaultWidth = defaultWidth;
        return this;
    }

    public LegendInfoBuilder withDefaultHeight(Integer defaultHeight) {
        this.defaultHeight = defaultHeight;
        return this;
    }

    public LegendInfoBuilder withDefaultFormat(String defaultFormat) {
        this.defaultFormat = defaultFormat;
        return this;
    }

    public LegendInfoBuilder withStyleName(String styleName) {
        this.styleName = styleName;
        return this;
    }

    public LegendInfoBuilder withWidth(Integer width) {
        this.width = width;
        return this;
    }

    public LegendInfoBuilder withHeight(Integer height) {
        this.height = height;
        return this;
    }

    public LegendInfoBuilder withFormat(String format) {
        this.format = format;
        return this;
    }

    public LegendInfoBuilder withUrl(String url) {
        this.url = url;
        return this;
    }

    public LegendInfoBuilder withCompleteUrl(String completeUrl) {
        this.completeUrl = completeUrl;
        return this;
    }

    public LegendInfo build() {
        // let's see if we need to really on the default values for width, height and format
        Integer finalWidth = width == null ? defaultWidth : width;
        Integer finalHeight = height == null ? defaultHeight : height;
        String finalFormat = format == null ? defaultFormat : format;
        // checking id we have correct values for width, height and format
        Preconditions.checkNotNull(finalWidth, "A legend width is mandatory.");
        Preconditions.checkNotNull(finalHeight, "A legend height is mandatory.");
        Preconditions.checkNotNull(finalHeight, "A legend image format is mandatory.");
        // default styles can have a NULL name
        String finalStyleName = styleName == null ? "" : styleName;
        // building the legend url
        String finalUrl = buildFinalUrl(finalStyleName, finalWidth, finalHeight, finalFormat);
        return new LegendInfo(finalStyleName, finalWidth, finalHeight, finalFormat, finalUrl);
    }

    /**
     * Helper method that builds the legend get url using the available info.
     */
    private String buildFinalUrl(String finalStyleName, Integer finalWidth, Integer finalHeight, String finalFormat) {
        if (completeUrl != null) {
            // we have a complete url so let's just return it
            return completeUrl;
        }
        String finalUrl = url == null ? layerUrl : url;
        // the legend url and layer name are mandatory
        Preconditions.checkNotNull(finalUrl, "A legend url is mandatory.");
        Preconditions.checkNotNull(layerName, "A layer name is mandatory.");
        return finalUrl + addQuoteMark(finalUrl) +
                "service=WMS&request=GetLegendGraphic" +
                "&format=" + finalFormat +
                "&width=" + finalWidth +
                "&height=" + finalHeight +
                "&layer=" + ServletUtils.URLEncode(layerName) +
                "&style=" + ServletUtils.URLEncode(finalStyleName);
    }

    /**
     * Helper method check's if a quote separating the base url from the query parameters
     * needs to be added to the url or not.
     */
    private String addQuoteMark(String finalUrl) {
        if (finalUrl.indexOf("?") == finalUrl.length() - 1) {
            // we are good no quote needed
            return "";
        }
        return "?";
    }
}