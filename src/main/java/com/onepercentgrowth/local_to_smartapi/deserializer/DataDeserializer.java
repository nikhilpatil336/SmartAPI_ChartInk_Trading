package com.onepercentgrowth.local_to_smartapi.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.onepercentgrowth.local_to_smartapi.model.OrderResponse;

import java.io.IOException;

public class DataDeserializer extends JsonDeserializer<OrderResponse.Data> {

    @Override
    public OrderResponse.Data deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonNode node = p.getCodec().readTree(p);

        if (node.isTextual() && node.asText().isEmpty()) {
            return null; // handle ""
        }

        return p.getCodec().treeToValue(node, OrderResponse.Data.class);
    }
}
