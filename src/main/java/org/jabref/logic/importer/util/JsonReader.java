package org.jabref.logic.importer.util;

import java.io.IOException;
import java.io.InputStream;

import org.jabref.logic.importer.ParseException;

import com.google.common.base.Charsets;
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
            String inputStr = new String((inputStream.readAllBytes()), Charsets.UTF_8);
            // Fallback: in case an empty stream was passed, return an empty JSON object
            if (inputStr.isBlank()) {
                return new JSONObject();
            }
            return new JSONObject(inputStr);
        } catch (IOException | JSONException e) {
            throw new ParseException(e);
        }
    }
}
