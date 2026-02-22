package com.onepercentgrowth.local_to_smartapi.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.onepercentgrowth.local_to_smartapi.model.RmsData;

import java.io.IOException;

public class RmsDataDeserializer extends JsonDeserializer<RmsData> {

    @Override
    public RmsData deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {

        JsonToken token = p.currentToken();

        // Handle: "data": ""
        if (token == JsonToken.VALUE_STRING &&
                p.getText().trim().isEmpty()) {
            return null;
        }

        // Handle: "data": null
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        // Normal object
        return p.readValueAs(RmsData.class);
    }
}

