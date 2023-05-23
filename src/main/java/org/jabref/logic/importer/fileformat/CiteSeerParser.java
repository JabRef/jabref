package org.jabref.logic.importer.fileformat;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import kong.unirest.json.JSONArray;

public class CiteSeerParser {

//    private static final Pattern PAPERID = Pattern.compile("pid/[0-9a-zA-Z]+", Pattern.MULTILINE);

    private void appendData(String data, BibEntry entry, Pattern pattern, Field field) {
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            entry.setField(field, matcher.group(1));
        }
    }

    public List<BibEntry> parseCiteSeerResponse(JSONArray jsonResponse) throws ParseException {
        List<BibEntry> response = new ArrayList<>();
        CookieHandler.setDefault(new CookieManager());

        // this is a placeholder, 'json -> string -> json' conversion should not be allowed
        // I plan to reformat this with the kong.unirest JSON classes / library for consistency
        String jsonString = jsonResponse.toString();
        JsonElement jsonElement = JsonParser.parseString(jsonString);

        for (JsonElement element: jsonElement.getAsJsonArray()) {
            response.add(parseBibEntry(element.getAsJsonObject()));
        }

        return response;
    }

    private BibEntry parseBibEntry(JsonObject jsonObj) throws ParseException {
        try {
            BibEntry bibEntry = new BibEntry();

            bibEntry.setField(StandardField.TITLE,
                    Optional.ofNullable(jsonObj.get("title").getAsString())
                            .orElse(""));
            bibEntry.setField(StandardField.VENUE,
                    Optional.ofNullable(jsonObj.get("venue").getAsString())
                            .orElse(""));
            bibEntry.setField(StandardField.YEAR,
                    Optional.ofNullable(jsonObj.get("year").getAsString())
                            .orElse(""));
            bibEntry.setField(StandardField.PUBLISHER,
                    Optional.ofNullable(jsonObj.get("publisher").getAsString())
                            .orElse(""));
            bibEntry.setField(StandardField.ABSTRACT,
                    Optional.ofNullable(jsonObj.get("abstract").getAsString())
                            .orElse(""));
//            bibEntry.setField(StandardField.AUTHOR,
//                    Optional.ofNullable(jsonObj.get("authors").getAsJsonArray())
//                            .orElse(new JsonArray()).forEach());
            bibEntry.setField(StandardField.JOURNAL,
                    Optional.ofNullable(jsonObj.get("journal").getAsString())
                            .orElse(""));
            return bibEntry;
        } catch (JsonParseException exception) {
            throw new ParseException("CiteSeer API JSON format has changed ", exception);
        }
    }
}
