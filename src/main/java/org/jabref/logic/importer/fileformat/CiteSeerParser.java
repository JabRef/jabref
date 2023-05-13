package org.jabref.logic.importer.fileformat;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.text.MessageFormat;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CiteSeerParser implements Parser {

    private static final String BASE_SEARCH_URL = "https://citeseerx.ist.psu.edu/search_result";

    private static final String API_URL = "https://citeseerx.ist.psu.edu/api/search";

    private static final Pattern PAPERID = Pattern.compile("pid/[0-9a-zA-Z]+", Pattern.MULTILINE);

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

            if (!jsonElement.isJsonObject()) {
                return response;
            }

            JsonArray items = jsonElement.getAsJsonObject().getAsJsonArray("response");
            for (JsonElement item: items) {
                for (Map.Entry<String, JsonElement> entry : item.getAsJsonObject().entrySet()) {
                    response.add(parseBibEntry(entry.getValue().getAsJsonObject()));
                }
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
