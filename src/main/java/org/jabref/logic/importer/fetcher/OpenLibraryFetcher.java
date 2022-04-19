package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher for OpenLibrary.
 * <a href="https://openlibrary.org/dev/docs/api/books">API documentation</a>.
 */
public class OpenLibraryFetcher extends AbstractIsbnFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenLibraryFetcher.class);
    private static final String BASE_URL = "https://openlibrary.org";

    public OpenLibraryFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "OpenLibrary";
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        this.ensureThatIsbnIsValid(identifier);
        URIBuilder uriBuilder = new URIBuilder(BASE_URL + "/isbn/" + identifier + ".json");
        return uriBuilder.build().toURL();
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
            BibEntry entry = new BibEntry(StandardEntryType.Book);
            entry.setField(StandardField.AUTHOR, toAuthors(item.optJSONArray("authors")));
            entry.setField(StandardField.PAGES, item.optString("number_of_pages"));
            entry.setField(StandardField.ISBN,
                    Optional.ofNullable(item.optJSONArray("isbn_13")).map(array -> array.getString(0))
                            .or(() -> Optional.ofNullable(item.optJSONArray("isbn_10")).map(array -> array.getString(0)))
                            .orElse(""));
            entry.setField(StandardField.TITLE,
                    Optional.ofNullable(item.optString("full_title", null))
                            .or(() -> Optional.ofNullable(item.optString("title", null)))
                            .orElse(""));
            entry.setField(StandardField.SUBTITLE, item.optString("subtitle"));
            Optional<String> yearOpt = Date.parse(item.optString("publish_date")).flatMap(Date::getYear).map(year -> year.toString());
            yearOpt.ifPresent(year -> {
                entry.setField(StandardField.YEAR, year);
            });
            entry.setField(StandardField.PUBLISHER,
                    Optional.ofNullable(item.optJSONArray("publishers")).map(array -> array.getString(0))
                            .orElse(""));
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("CrossRef API JSON format has changed", exception);
        }
    }

    private String toAuthors(JSONArray authors) {
        if (authors == null) {
            return "";
        }
        return IntStream.range(0, authors.length())
                        .mapToObj(authors::getJSONObject)
                        .map(authorObject -> toAuthor(authorObject.getString("key")))
                        .collect(AuthorList.collect())
                        .getAsLastFirstNamesWithAnd(false);
    }

    private Author toAuthor(String key) {
        JsonNode authorResponse = Unirest.get(BASE_URL + key + ".json").asJson().getBody();
        if (authorResponse == null) {
            LOGGER.warn("Could not parse author");
            return new Author(null, null, null, null, null);
        }
        JSONObject result = authorResponse.getObject();
        Optional<String> nameOptional = Optional.ofNullable(result.optString("personal_name", null)).or(() -> Optional.ofNullable(result.optString("name", null)));
        if (nameOptional.isEmpty()) {
            LOGGER.warn("Could not parse author name");
            return new Author(null, null, null, null, null);
        }
        AuthorListParser authorListParser = new AuthorListParser();
        AuthorList authorList = authorListParser.parse(nameOptional.get());
        return authorList.getAuthor(0);
    }
}
