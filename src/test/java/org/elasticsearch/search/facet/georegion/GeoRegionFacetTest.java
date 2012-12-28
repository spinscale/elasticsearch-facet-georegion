package org.elasticsearch.search.facet.georegion;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.RandomStringGenerator;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GeoRegionFacetTest {

    public static final String INDEX = "myindex";
    public static final String TYPE = "mytype";
    public static final int NUMBER_OF_SHARDS = 5;

    private static Node node;
    private static Client client;

    private Map<String, double[]> cities = Maps.newConcurrentMap();

    @BeforeClass
    public static void createNodes() throws Exception {
        Builder builder = ImmutableSettings.settingsBuilder();
        builder = builder.loadFromClasspath("/elasticsearch.yml");

        builder.put("facet.georegion.file", "config/countries.geo.json");
        builder.put("path.logs", "data/logs");
        builder.put("index.number_of_shards", NUMBER_OF_SHARDS);
        builder.put("index.number_of_replicas", 0);
        builder.put("cluster.name", "test-cluster-" + RandomStringGenerator.randomAlphanumeric(12));

        LogConfigurator.configure(builder.build());

        node = nodeBuilder().settings(builder.build()).build().start();
        client = node.client();
    }

    @AfterClass
    public static void closeDown() {
        client.close();
        node.close();
    }

    @Before
    public void createIndexAndMappingAndData() {
        try {
            client.admin().indices().prepareDelete(INDEX).execute().actionGet();
        } catch (Exception e) {}

        node.client().admin().indices().prepareCreate(INDEX).execute().actionGet();
        node.client().admin().indices().preparePutMapping(INDEX)
                .setType(TYPE)
                .setSource("{ " + TYPE + " : { properties : { loc : { type : \"geo_point\", store : \"yes\" } } } }")
                .execute().actionGet();

        node.client().admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet();

        cities.put("Paris",         new double[] {48.856666666667, 2.3516666666667});
        cities.put("Berlin",        new double[] {52.518611111111, 13.40805555555});
        cities.put("Munich",        new double[] {48.136944444444, 11.575277777778});
        cities.put("Cologne",       new double[] {50.938055555556, 6.9569444444444});
        cities.put("Rome",          new double[] {41.883333333333, 12.483333333333});
        cities.put("Milano",        new double[] {45.4625, 9.1863888888889});
        cities.put("Stockholm",     new double[] {59.325, 18.05});
        cities.put("Amsterdam",     new double[] {52.370197222222, 4.8904444444444});
        cities.put("Kopenhagen",    new double[] {55.675706111111, 12.578745});
        cities.put("Zurich",        new double[] {47.3686498, 8.5391825});
        // Dirty hack: Canada is not correctly loaded, so we can have "_unknown" as reply for montreal
        cities.put("Montreal",        new double[] {45.5086699, -73.5539925});

        for (String city : cities.keySet()) {
            String json = String.format("{ \"name\": \"%s\", \"loc\" : [%s, %s] }", city, cities.get(city)[0], cities.get(city)[1]);
            client.prepareIndex(INDEX, TYPE, city).setSource(json).execute().actionGet();
        }

        // Indexing one city without coordinates to show the _missing feature
        client.prepareIndex(INDEX, TYPE, "Trondheim").setSource("{ \"name\": \"Trondheim\" }").execute().actionGet();

        client.admin().indices().prepareRefresh().execute().actionGet();
    }

    @Test
    public void testGeoRegionFacet() throws Exception {
        BytesReference facetBuilderRef = JsonXContent.contentBuilder()
                .startObject()
                .startObject("regions")
                .startObject("georegion")
                .field("field", "loc")
                .field("region", "countries")
                .endObject()
                .endObject()
                .endObject()
                .bytes();

        GeoRegionFacet facet = getFacetFromQuery(QueryBuilders.matchAllQuery(), facetBuilderRef);

        TreeMap<String, AtomicLong> sortedMap = new TreeMap(facet.counts());
        assertThat(sortedMap.toString(), is("{Denmark=1, France=1, Germany=3, Italy=2, Netherlands=1, Sweden=1, Switzerland=1, _missing=1, _unknown=1}"));
    }

    @Test
    public void testFacettingWorksWithFacetFilter() throws Exception {
        BytesReference facetBuilderRef = JsonXContent.contentBuilder()
                .startObject()
                .startObject("regions")
                .startObject("georegion")
                .field("field", "loc")
                .field("region", "countries")
                .endObject()
                .startObject("facet_filter")
                .startObject("term")
                .field("name", "berlin")
                .endObject()
                .endObject()
                .endObject()
                .endObject()
                .bytes();

        GeoRegionFacet facet = getFacetFromQuery(QueryBuilders.matchAllQuery(), facetBuilderRef);

        TreeMap<String, AtomicLong> sortedMap = new TreeMap(facet.counts());
        assertThat(sortedMap.toString(), is("{Germany=1}"));
    }

    private GeoRegionFacet getFacetFromQuery(MatchAllQueryBuilder matchAllQueryBuilder, BytesReference facetBuilderRef) {
        SearchResponse searchResponse = new SearchRequestBuilder(client)
                .setIndices(INDEX)
                .setTypes(TYPE)
                .setQuery(QueryBuilders.matchAllQuery())
                .setFacets(facetBuilderRef)
                .execute().actionGet();

        return searchResponse.facets().facet("regions");
    }
}
