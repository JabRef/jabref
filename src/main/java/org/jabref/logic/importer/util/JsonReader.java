package org.jabref.logic.importer.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.importer.ParseException;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;

/**
 * Converts an {@link InputStream} into a {@link JSONObject}.
 */
public class JsonReader {

    /**
     * Converts the given input stream into a {@link JSONObject}.
     *
     * @return A {@link JSONObject}. An empty JSON object is returned in the case an empty stream is passed.
     */
    public static JSONObject toJsonObject(InputStream inputStream) throws ParseException {
        try {
            String inputStr = new String((inputStream.readAllBytes()), StandardCharsets.UTF_8);
            // Fallback: in case an empty stream was passed, return an empty JSON object
            if (inputStr.isBlank()) {
                return new JSONObject();
            }
            return new JSONObject(inputStr);
        } catch (IOException | JSONException e) {
            throw new ParseException(e);
        }
    }

    public static JSONArray toJsonArray(InputStream stream) throws ParseException {
        try {
            String inpStr = new String((stream.readAllBytes()), StandardCharsets.UTF_8);
            if (inpStr.isBlank()) {
                return new JSONArray();
            }
            return new JSONArray(inpStr);
        } catch (IOException | JSONException e) {
            throw new ParseException(e);
        }
    }
}
