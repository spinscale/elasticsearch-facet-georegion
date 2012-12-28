package org.elasticsearch.search.facet.georegion;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.search.facet.Facet;
import org.elasticsearch.search.facet.InternalFacet;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class GeoRegionFacet implements InternalFacet {
    public static final String TYPE = "georegion";
    private static final String STREAM_TYPE = "georegion";

    private String name;
    private Map<String, AtomicLong> counts = Maps.newHashMap();


    public static void registerStreams() {
        Streams.registerStream(STREAM, STREAM_TYPE);
    }

    static InternalFacet.Stream STREAM = new InternalFacet.Stream() {
        @Override
        public Facet readFacet(String type, StreamInput in) throws IOException {
            GeoRegionFacet facet = new GeoRegionFacet();
            facet.readFrom(in);
            return facet;
        }
    };

    private GeoRegionFacet() {}

    public GeoRegionFacet(String name, Map<String, AtomicLong> counts) {
        this.name = name;
        this.counts = counts;
    }

    @Override
    public String streamType() {
        return STREAM_TYPE;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public String getType() {
        return type();
    }

    public Map<String, AtomicLong> getCounts() {
        return counts;
    }

    public Map<String, AtomicLong> counts() {
        return counts;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        name = in.readString();

        long size = in.readVLong();
        for (int i = 0; i < size; i++) {
            counts.put(in.readString(), new AtomicLong(in.readVLong()));
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(name);
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
        builder.startObject(name);
        builder.field(Fields._TYPE, STREAM_TYPE);
        builder.field("counts", counts);
        builder.endObject();
        return builder;
    }
}
