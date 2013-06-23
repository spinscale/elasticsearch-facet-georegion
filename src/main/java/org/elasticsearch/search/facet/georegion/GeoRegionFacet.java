package org.elasticsearch.search.facet.georegion;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.InternalFacet;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class GeoRegionFacet extends InternalFacet {

    public static final String TYPE = "georegion";
    private static final BytesReference STREAM_TYPE = new BytesArray("georegion");

    private Map<String, AtomicLong> counts = Maps.newHashMap();


    public static void registerStreams() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static InternalFacet.Stream STREAM = new InternalFacet.Stream() {
        @Override
        public Facet readFacet(StreamInput in) throws IOException {
            GeoRegionFacet facet = new GeoRegionFacet();
            facet.readFrom(in);
            return facet;
        }
    };

    private GeoRegionFacet() {}

    public GeoRegionFacet(String name, Map<String, AtomicLong> counts) {
        super(name);
        this.counts = counts;
    }

    @Override
    public BytesReference streamType() {
        return STREAM_TYPE;
    }

    @Override
    public Facet reduce(List<Facet> facets) {
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

    @Override
    public String getType() {
        return TYPE;
    }

    public Map<String, AtomicLong> getCounts() {
        return counts;
    }

    public Map<String, AtomicLong> counts() {
        return counts;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);

        long size = in.readVLong();
        for (int i = 0; i < size; i++) {
            counts.put(in.readString(), new AtomicLong(in.readVLong()));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVLong(counts.size());
        for (Map.Entry<String, AtomicLong> entry : counts.entrySet()) {
             out.writeString(entry.getKey());
             out.writeVLong(entry.getValue().get());
        }
    }

    // TODO: IS THIS NEEDED?
    static final class Fields {
        static final XContentBuilderString _TYPE = new XContentBuilderString("_type");
        static final XContentBuilderString FACET = new XContentBuilderString("facet");
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(getName());
        builder.field(Fields._TYPE, STREAM_TYPE);
        builder.field("counts", counts);
        builder.endObject();
        return builder;
    }
}
