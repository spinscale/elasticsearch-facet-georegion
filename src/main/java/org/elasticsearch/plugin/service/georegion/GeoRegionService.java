package org.elasticsearch.plugin.service.georegion;

import com.spatial4j.core.shape.Shape;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.geo.GeoJSONShapeParser;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class GeoRegionService extends AbstractLifecycleComponent<GeoRegionService> {

    public static final Map<String, Map<String, Shape>> shapes = Maps.newConcurrentMap();

    @Inject
    public GeoRegionService(Settings settings) {
        super(settings);
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        shapes.put("countries", Maps.<String, Shape>newConcurrentMap());

        try {
            // load the worlds json file
            String path = settings.get("facet.georegion.file");
            FileInputStream fis = new FileInputStream(path);
            XContentParser parser = XContentHelper.createParser(new BytesArray(Streams.copyToByteArray(fis)));

            String countryName = null;
            Map<String, Object> geometry = null;

            XContentParser.Token token = null;
            while ((token = parser.nextToken()) != null) {
                if ("name".equals(parser.text())) {
                    parser.nextToken();
                    countryName = parser.text();
                } else if ("geometry".equals(parser.text())) {
                    parser.nextToken();
                    try {
                        // TODO: THIS LOOKS AS IF LAT/LON ARE INDEXED WRONG WAY ROUND
                        Shape shape = GeoJSONShapeParser.parse(parser);
                        shapes.get("countries").put(countryName, shape);
                    } catch (AssertionError e) {
                        logger.error("Could not add country {} to countries with message: {}", countryName, e.getMessage());
                    } catch (Exception e) {
                        logger.error("Could not add country {} to countries with message: {}", countryName, e.getMessage());
                    }
                }
            }

        } catch (FileNotFoundException e) {
            throw new ElasticSearchException("File not found", e);
        } catch (IOException e) {
            throw new ElasticSearchException("IOExc", e);
        }

        for (String name : shapes.keySet()) {
            logger.info("Loaded {} elements into region {}", shapes.get(name).size(), name);
        }
    }

    @Override
    protected void doStop() throws ElasticSearchException {}

    @Override
    protected void doClose() throws ElasticSearchException {}
}
