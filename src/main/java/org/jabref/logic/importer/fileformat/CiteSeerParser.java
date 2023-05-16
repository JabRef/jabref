package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CiteSeerParser implements Parser {

//    private static final Pattern PAPERID = Pattern.compile("pid/[0-9a-zA-Z]+", Pattern.MULTILINE);

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        List<BibEntry> bibEntries;
        try {
            bibEntries = parseCiteSeerResponse(inputStream);
        } catch (FetcherException e) {
            throw new ParseException("Could not parse CiteSeer data, ", e);
        }
        return bibEntries;
    }

    private void appendData(String data, BibEntry entry, Pattern pattern, Field field) {
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            entry.setField(field, matcher.group(1));
        }
    }

    private List<BibEntry> parseCiteSeerResponse(InputStream inputStream) throws FetcherException {
        List<BibEntry> response = new ArrayList<>();
        CookieHandler.setDefault(new CookieManager());

        try {
            String jsonString = new String((inputStream.readAllBytes()), StandardCharsets.UTF_8);
            JsonElement jsonElement = JsonParser.parseString(jsonString);

            for (JsonElement element: jsonElement.getAsJsonArray()) {
                response.add(parseBibEntry(element.getAsJsonObject()));
//                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
//                    response.add(parseBibEntry(entry.getValue().getAsJsonObject()));
//                }
            }
        } catch (IOException ex) {
            throw new FetcherException("Unable to parse input stream into json object: ", ex);
        }

        return response;
    }

    private BibEntry parseBibEntry(JsonObject jsonObj) {
        BibEntry bibEntry = new BibEntry();

        if (jsonObj.has("title")) {
            bibEntry.setField(StandardField.TITLE, jsonObj.get("title").getAsString());
        }

        if (jsonObj.has("venue")) {
            bibEntry.setField(StandardField.VENUE, jsonObj.get("venue").getAsString());
        }

        if (jsonObj.has("year")) {
            bibEntry.setField(StandardField.YEAR, jsonObj.get("year").getAsString());
        }

        if (jsonObj.has("publisher")) {
            bibEntry.setField(StandardField.PUBLISHER, jsonObj.get("publisher").getAsString());
        }

        if (jsonObj.has("abstract")) {
            bibEntry.setField(StandardField.ABSTRACT, jsonObj.get("abstract").getAsString());
        }

        if (jsonObj.has("author")) {
            bibEntry.setField(StandardField.AUTHOR, jsonObj.get("author").getAsString());
        }

        if (jsonObj.has("journal")) {
            bibEntry.setField(StandardField.JOURNAL, jsonObj.get("journal").getAsString());
        }

        return bibEntry;
    }
}
