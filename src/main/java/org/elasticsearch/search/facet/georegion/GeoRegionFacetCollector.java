package org.elasticsearch.search.facet.georegion;

import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.SpatialRelation;
import org.apache.lucene.index.IndexReader;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.geo.ShapeBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.cache.field.data.FieldDataCache;
import org.elasticsearch.index.field.data.FieldData;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.geo.GeoPointFieldData;
import org.elasticsearch.index.mapper.geo.GeoPointFieldDataType;
import org.elasticsearch.index.search.geo.GeoHashUtils;
import org.elasticsearch.plugin.service.georegion.GeoRegionService;
import org.elasticsearch.search.facet.AbstractFacetCollector;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.FacetPhaseExecutionException;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class GeoRegionFacetCollector extends AbstractFacetCollector implements FieldData.StringValueInDocProc {

    final static ESLogger logger = Loggers.getLogger(GeoRegionFacetCollector.class);

    private final String indexFieldName;
    private final FieldDataCache fieldDataCache;
    protected GeoPointFieldData fieldData;

    private Map<String, AtomicLong> counts = Maps.newHashMap();
    private final String region;

    public GeoRegionFacetCollector(String facetName, String fieldName, String region, SearchContext searchContext) {
        super(facetName);
        this.fieldDataCache = searchContext.fieldDataCache();
        this.region = region;

        MapperService.SmartNameFieldMappers smartMappers = searchContext.smartFieldMappers(fieldName);
        if (smartMappers == null || !smartMappers.hasMapper()) {
            throw new FacetPhaseExecutionException(facetName, "No mapping found for field [" + fieldName + "]");
        }
        if (smartMappers.mapper().fieldDataType() != GeoPointFieldDataType.TYPE) {
            throw new FacetPhaseExecutionException(facetName, "field [" + fieldName + "] is not a geo_point field");
        }

        // add type filter if there is exact doc mapper associated with it
        if (smartMappers.explicitTypeInNameWithDocMapper()) {
            setFilter(searchContext.filterCache().cache(smartMappers.docMapper().typeFilter()));
        }

        indexFieldName = smartMappers.mapper().names().indexName();
    }


    @Override
    protected void doSetNextReader(IndexReader reader, int docBase)
            throws IOException {
        fieldData = (GeoPointFieldData) fieldDataCache.cache(GeoPointFieldDataType.TYPE, reader, indexFieldName);
    }

    @Override
    protected void doCollect(int doc) throws IOException {
        logger.debug("collecting {}", doc);
        fieldData.forEachValueInDoc(doc, this);
    }

    public void onValue(int docId, String value) {
        double[] location = GeoHashUtils.decode(value);
        Point point = ShapeBuilder.newPoint(location[0], location[1]);
        for (Map.Entry<String, Shape> entry : GeoRegionService.shapes.get(region).entrySet()) {
            String name = entry.getKey();
            Shape shape = entry.getValue();

            if (shape.relate(point) == SpatialRelation.CONTAINS) {
                addOrIncrement(name, 1L);
                return;
            }
        }

        addOrIncrement("_unknown", 1L);
    }

    public void onMissing(int docId) {
        addOrIncrement("_missing", 1L);
    }

    private void addOrIncrement(String name, Long value) {
        if (counts.containsKey(name)) {
            counts.get(name).addAndGet(value);
        } else {
            counts.put(name, new AtomicLong(value));
        }
    }

    @Override
    public Facet facet() {
        return new GeoRegionFacet(facetName, counts);
    }

}
