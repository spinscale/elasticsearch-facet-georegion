package org.elasticsearch.search.facet.georegion;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetCollector;
import org.elasticsearch.search.facet.FacetPhaseExecutionException;
import org.elasticsearch.search.facet.FacetProcessor;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.elasticsearch.common.collect.Lists.newArrayList;

/**
 *
 */
public class GeoRegionFacetProcessor extends AbstractComponent implements FacetProcessor {

    final static ESLogger logger = Loggers.getLogger(GeoRegionFacetProcessor.class);

    @Inject
    public GeoRegionFacetProcessor(Settings settings) {
        super(settings);
        GeoRegionFacet.registerStreams();
    }

    public String[] types() {
        return new String[] { GeoRegionFacet.TYPE };
    }

    public FacetCollector parse(String facetName, XContentParser parser,
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

        return new GeoRegionFacetCollector(facetName, field, region, context);
    }

    public Facet reduce(String facetName, List<Facet> facets) {
        GeoRegionFacet geoRegionFacet = (GeoRegionFacet) facets.get(0);

        for (int i = 1 ; i < facets.size() ; i++) {
            Facet facet = facets.get(i);
            if (facet instanceof GeoRegionFacet) {
                GeoRegionFacet regionFacet = (GeoRegionFacet) facet;
                for (Map.Entry<String, AtomicLong> entry : regionFacet.counts().entrySet()) {
                    if (geoRegionFacet.counts().containsKey(entry.getKey())) {
                        geoRegionFacet.counts().get(entry.getKey()).addAndGet(entry.getValue().longValue());
                    } else {
                        geoRegionFacet.counts().put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        return geoRegionFacet;
    }

}
