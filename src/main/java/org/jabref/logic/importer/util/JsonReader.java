package org.jabref.logic.importer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.importer.ParseException;

import org.json.JSONObject;

/**
 * Converts an {@link InputStream} into a {@link JSONObject}.
 */
public class JsonReader {

    public static JSONObject toJsonObject(InputStreamReader input) throws ParseException {
        BufferedReader streamReader = new BufferedReader(input);
        StringBuilder responseStrBuilder = new StringBuilder();

        try {
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
            return new JSONObject(responseStrBuilder.toString());
        } catch (IOException e) {
            throw new ParseException(e);
        }
    }

    public static JSONObject toJsonObject(InputStream input) throws ParseException {
        return toJsonObject(new InputStreamReader(input, StandardCharsets.UTF_8));
    }
}
