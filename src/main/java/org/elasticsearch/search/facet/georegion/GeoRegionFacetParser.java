package org.elasticsearch.search.facet.georegion;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.search.facet.FacetExecutor;
import org.elasticsearch.search.facet.FacetParser;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;

/**
 *
 */
public class GeoRegionFacetParser extends AbstractComponent implements FacetParser {

    final static ESLogger logger = Loggers.getLogger(GeoRegionFacetParser.class);

    @Inject
    public GeoRegionFacetParser(Settings settings) {
        super(settings);
        GeoRegionFacet.registerStreams();
    }

    public String[] types() {
        return new String[] { GeoRegionFacet.TYPE };
    }

    @Override
    public FacetExecutor.Mode defaultMainMode() {
        return FacetExecutor.Mode.COLLECTOR;
    }

    @Override
    public FacetExecutor.Mode defaultGlobalMode() {
        return FacetExecutor.Mode.COLLECTOR;
    }

    public FacetExecutor parse(String facetName, XContentParser parser,
                                SearchContext context) throws IOException {
        String field = "location";
        String region = null;

        String currentFieldName = null;
        XContentParser.Token token;

        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (token.isValue()) {
                if ("field".equals(currentFieldName)) {
                    field = parser.text();
                } else if ("region".equals(currentFieldName)) {
                    region = parser.text();
                }
            }
        }

        if (region == null || region.length() == 0) {
            throw new ElasticSearchException("Region field not set in region facet");
        }

        return new GeoRegionFacetExecutor(facetName, field, region, context);
    }

}
