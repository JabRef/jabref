package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for ISBN using <a href="https://doi-to-bibtex-converter.herokuapp.com">doi-to-bibtex-converter.herokuapp</a>.
 */
public class DoiToBibtexConverterComIsbnFetcher extends AbstractIsbnFetcher {
    private static final String BASE_URL = "https://doi-to-bibtex-converter.herokuapp.com";

    public DoiToBibtexConverterComIsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "ISBN (doi-to-bibtex-converter.herokuapp.com)";
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        this.ensureThatIsbnIsValid(identifier);
        return new URIBuilder(BASE_URL)
                .setPathSegments("getInfo.php")
                .setParameter("query", identifier)
                .setParameter("format", "json")
                .build()
                .toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            if (response.isEmpty()) {
                return Collections.emptyList();
            }

            String error = response.optString("error");
            if (StringUtil.isNotBlank(error)) {
                throw new ParseException(error);
            }

            BibEntry entry = jsonItemToBibEntry(response);
            return List.of(entry);
        };
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            JSONArray data = item.optJSONArray("data");
            var type = getElementFromJSONArrayByKey(data, "type");

            BibEntry entry = new BibEntry(evaluateBibEntryTypeFromString(type));
            entry.setField(StandardField.AUTHOR, getElementFromJSONArrayByKey(data, "author"));
            entry.setField(StandardField.PAGES, getElementFromJSONArrayByKey(data, "pagecount"));
            entry.setField(StandardField.ISBN, getElementFromJSONArrayByKey(data, "isbn"));
            entry.setField(StandardField.TITLE, getElementFromJSONArrayByKey(data, "title"));
            entry.setField(StandardField.YEAR, getElementFromJSONArrayByKey(data, "year"));
            entry.setField(StandardField.MONTH, getElementFromJSONArrayByKey(data, "month"));
            entry.setField(StandardField.DAY, getElementFromJSONArrayByKey(data, "day"));
            return entry;
        } catch (
                JSONException exception) {
            throw new ParseException("CrossRef API JSON format has changed", exception);
        }
    }

    private String getElementFromJSONArrayByKey(JSONArray jsonArray, String key) {
        return IntStream.range(0, jsonArray.length())
                        .mapToObj(jsonArray::getJSONObject)
                        .map(obj -> obj.getString(key))
                        .findFirst()
                        .orElse("");
    }

    private StandardEntryType evaluateBibEntryTypeFromString(String type) {
        return Stream.of(StandardEntryType.values())
                     .filter(entryType -> entryType.name().equalsIgnoreCase(type))
                     .findAny()
                     .orElse(StandardEntryType.Book);
    }
}
