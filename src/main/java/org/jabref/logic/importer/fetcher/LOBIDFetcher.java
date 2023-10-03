package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.LOBIDQueryTransformer;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the LOBID API
 *
 * @see <a href="https://lobid.org/resources/api">API documentation</a> for more details
 */
public class LOBIDFetcher implements PagedSearchBasedParserFetcher {

    public static final String FETCHER_NAME = "LOBID";

    private static final Logger LOGGER = LoggerFactory.getLogger(LOBIDFetcher.class);

    private static final String API_URL = "https://lobid.org/resources/search";

    private final ImporterPreferences importerPreferences;

    public LOBIDFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    /**
     * Gets the query URL
     *
     * @param luceneQuery the search query
     * @param pageNumber  the number of the page indexed from 0
     * @return URL
     */
    @Override
    public URL getURLForQuery(QueryNode luceneQuery, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("q", new LOBIDQueryTransformer().transformLuceneQuery(luceneQuery).orElse("")); // search query
        uriBuilder.addParameter("from", String.valueOf(getPageSize() * pageNumber)); // from entry number, starts indexing at 0
        uriBuilder.addParameter("size", String.valueOf(getPageSize())); // page size
        uriBuilder.addParameter("format", "json"); // response format
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            JSONObject jsonObject = new JSONObject(response);

            List<BibEntry> entries = new ArrayList<>();
            if (jsonObject.has("member")) {
                JSONArray results = jsonObject.getJSONArray("member");
                for (int i = 0; i < results.length(); i++) {
                    JSONObject jsonEntry = results.getJSONObject(i);
                    BibEntry entry = parseJSONtoBibtex(jsonEntry);
                    entries.add(entry);
                }
            }

            return entries;
        };
    }

    private BibEntry parseJSONtoBibtex(JSONObject jsonEntry) {
        BibEntry entry = new BibEntry();
        Field nametype = StandardField.JOURNAL;
        EntryType entryType = StandardEntryType.InCollection;

        // publication type
        JSONArray typeArray = jsonEntry.optJSONArray("type");
        String types = "";
        if (typeArray != null) {
            List<String> typeList = IntStream.range(0, typeArray.length())
                                             .mapToObj(typeArray::optString)
                                             .filter(type -> !type.isEmpty())
                                             .toList();
            types = String.join(", ", typeList);
            entry.setField(StandardField.TYPE, types);
        }

        if (types.toLowerCase().contains("book")) {
            entryType = StandardEntryType.Book;
            nametype = StandardField.BOOKTITLE;
        } else if (types.toLowerCase().contains("article")) {
            entryType = StandardEntryType.Article;
        }
        entry.setType(entryType);

        // isbn
        String isbn = getFirstArrayElement(jsonEntry, "isbn");
        entry.setField(StandardField.ISBN, isbn);

        // parent resource
        String bibliographicCitation = jsonEntry.optString("bibliographicCitation", "");
        String[] bibSplit = bibliographicCitation.split("/");
        String parentResource = "";
        if (bibSplit.length > 0) {
            parentResource = bibSplit[0].trim();
            entry.setField(nametype, parentResource);
        }

        entry.setField(StandardField.ISSN, getFirstArrayElement(jsonEntry, "issn"));
        entry.setField(StandardField.TITLE, jsonEntry.optString("title", ""));
        entry.setField(StandardField.ABSTRACT, getFirstArrayElement(jsonEntry, "note"));
        entry.setField(StandardField.TITLEADDON, getFirstArrayElement(jsonEntry, "otherTitleInformation"));
        entry.setField(StandardField.EDITION, getFirstArrayElement(jsonEntry, "edition"));

        // authors
        JSONArray authors = jsonEntry.optJSONArray("contribution");
        if (authors != null) {
            List<String> authorNames = getAuthorNames(authors);
            if (!authors.isEmpty()) {
                entry.setField(StandardField.AUTHOR, String.join(" and ", authorNames));
            }
        }

        // publication
        Optional.ofNullable(jsonEntry.optJSONArray("publication"))
                .map(array -> array.getJSONObject(0))
                .ifPresent(publication -> {
                    entry.setField(StandardField.PUBLISHER, getFirstArrayElement(publication, "publishedBy"));
                    entry.setField(StandardField.LOCATION, getFirstArrayElement(publication, "location"));
                    String date = publication.optString("startDate");
                    entry.setField(StandardField.DATE, date);
                    entry.setField(StandardField.YEAR, date);
                });

        // url
        JSONObject describedBy = jsonEntry.optJSONObject("describedBy");
        if (describedBy != null) {
            entry.setField(StandardField.URL, describedBy.optString("id"));
        }

        // language
        JSONArray languageArray = jsonEntry.optJSONArray("language");
        if (languageArray != null) {
            List<String> languageList = IntStream.range(0, languageArray.length())
                                                 .mapToObj(languageArray::getJSONObject)
                                                 .filter(Objects::nonNull)
                                                 .map(language -> language.optString("label"))
                                                 .toList();
            entry.setField(StandardField.LANGUAGE, String.join(" and ", languageList));
        }

        // keywords
        JSONArray keywordArray = jsonEntry.optJSONArray("subjectslabels");
        if (keywordArray != null) {
            List<String> keywordList = IntStream.range(0, keywordArray.length())
                                             .mapToObj(keywordArray::optString)
                                             .filter(keyword -> !keyword.isEmpty())
                                             .toList();
            entry.setField(StandardField.KEYWORDS, String.join(", ", keywordList));
        }

        return entry;
    }

    private static List<String> getAuthorNames(JSONArray authors) {
        return IntStream.range(0, authors.length())
                        .mapToObj(authors::getJSONObject)
                        .map(author -> author.optJSONObject("agent"))
                        .filter(Objects::nonNull)
                        .map(agent -> agent.optString("label"))
                        .toList();
    }

    private static String getFirstArrayElement(JSONObject jsonEntry, String key) {
        return Optional.ofNullable(jsonEntry.optJSONArray(key))
                       .map(array -> array.getString(0))
                       .orElse("");
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
