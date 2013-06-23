package org.elasticsearch.search.facet.georegion;

import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.SpatialRelation;
import org.apache.lucene.index.AtomicReaderContext;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.ShapeBuilder;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.fielddata.GeoPointValues;
import org.elasticsearch.index.fielddata.IndexGeoPointFieldData;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.plugin.service.georegion.GeoRegionService;
import org.elasticsearch.search.facet.FacetExecutor;
import org.elasticsearch.search.facet.FacetPhaseExecutionException;
import org.elasticsearch.search.facet.InternalFacet;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class GeoRegionFacetExecutor extends FacetExecutor {

    private final static ESLogger logger = Loggers.getLogger(GeoRegionFacetExecutor.class);
    private final static FieldDataType REQUIRED_FIELD_DATA_TYPE = new FieldDataType("geo_point");

    final IndexGeoPointFieldData indexFieldData;
    private final String indexFieldName;
    private final SearchContext searchContext;

    private Map<String, AtomicLong> counts = Maps.newHashMap();
    private final String region;

    public GeoRegionFacetExecutor(String facetName, String fieldName, String region, SearchContext searchContext) {
        this.searchContext = searchContext;
        this.region = region;
        this.indexFieldData = searchContext.fieldData().getForField(searchContext.smartNameFieldMapper(fieldName));


        MapperService.SmartNameFieldMappers smartMappers = searchContext.smartFieldMappers(fieldName);
        if (smartMappers == null || !smartMappers.hasMapper()) {
            throw new FacetPhaseExecutionException(facetName, "No mapping found for field [" + fieldName + "]");
        }
        if (!smartMappers.mapper().fieldDataType().equals(REQUIRED_FIELD_DATA_TYPE)) {
            throw new FacetPhaseExecutionException(facetName, "field [" + fieldName + "] is not a geo_point field");
        }

        // TODO: is this ok to leave out?
        // add type filter if there is exact doc mapper associated with it
        //if (smartMappers.explicitTypeInNameWithDocMapper()) {
        //    setFilter(searchContext.filterCache().cache(smartMappers.docMapper().typeFilter()));
        //}

        indexFieldName = smartMappers.mapper().names().indexName();
    }

    @Override
    public InternalFacet buildFacet(String facetName) {
        return new GeoRegionFacet(facetName, counts);
    }

    @Override
    public Collector collector() {
        return new Collector();
    }

    final class Collector extends FacetExecutor.Collector {

        protected GeoPointValues values;

        @Override
        public void postCollection() {
        }

        @Override
        public void collect(int docId) throws IOException {
            final GeoPointValues.Iter iter = values.getIter(docId);

            if (iter == null || !iter.hasNext()) {
                addOrIncrement("_missing", 1L);
                return;
            }

            while (iter.hasNext()) {
                final GeoPoint geoPoint = iter.next();
                // TODO: THIS LOOKS AS IF LAT/LON ARE INDEXED WRONG WAY ROUND
                Point point = ShapeBuilder.newPoint(geoPoint.lat(), geoPoint.lon());
                for (Map.Entry<String, Shape> entry : GeoRegionService.shapes.get(region).entrySet()) {
                    String name = entry.getKey();
                    Shape shape = entry.getValue();

                    if (shape.relate(point) == SpatialRelation.CONTAINS) {
                        addOrIncrement(name, 1L);
                        return;
                    }
                }
            }

            addOrIncrement("_unknown", 1L);
        }

        @Override
        public void setNextReader(AtomicReaderContext context) throws IOException {
            values = indexFieldData.load(context).getGeoPointValues();
        }

        private void addOrIncrement(String name, Long value) {
            if (counts.containsKey(name)) {
                counts.get(name).addAndGet(value);
            } else {
                counts.put(name, new AtomicLong(value));
            }
        }
    }

    /*
    @Override
    protected void doSetNextReader(IndexReader reader, int docBase)
            throws IOException {
        //fieldData = (GeoPointFieldData) fieldDataCache.cache(GeoPointFieldDataType.TYPE, reader, indexFieldName);
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
    */

    /*
    @Override
    public Facet facet() {
        return new GeoRegionFacet(facetName, counts);
    }
    */

}
