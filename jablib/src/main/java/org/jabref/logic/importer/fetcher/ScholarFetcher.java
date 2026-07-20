package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.transformers.ScholarApiQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.paging.Page;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScholarFetcher implements PagedSearchBasedFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "Scholar";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScholarFetcher.class);

    private static final String LIST_URL = "https://scholarapi.net/api/v1/list";

    private final Map<PageKey, String> cursorCacheMap = new ConcurrentHashMap<>();

    private final ImporterPreferences importerPreferences;

    public ScholarFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    /// Convert a JSONObject obtained from <a href="https://scholarapi.net/docs/api">the Scholar API</a> to a BibEntry
    ///
    /// @param scholarJsonEntry the JSONObject from search results
    /// @return the converted BibEntry
    public static BibEntry jsonItemToBibEntry(JSONObject scholarJsonEntry) throws ParseException {
        try {
            BibEntry entry = new BibEntry(StandardEntryType.Article);

            if (scholarJsonEntry.has("authors")) {
                JSONArray authors = scholarJsonEntry.getJSONArray("authors");
                List<String> authorsList = new ArrayList<>();
                for (int i = 0; i < authors.length(); i++) {
                    authorsList.add(authors.getString(i));
                }
                if (!authorsList.isEmpty()) {
                    entry.setField(StandardField.AUTHOR, String.join(" and ", authorsList));
                } else {
                    LOGGER.info("Empty authors array.");
                }
            } else {
                LOGGER.info("No authors found.");
            }

            // direct accessible fields
            entry.setField(StandardField.TITLE, scholarJsonEntry.getString("title"));
            String publishedDate = scholarJsonEntry.getString("published_date");
            String publishedDateOnly = publishedDate.split("T")[0];
            entry.setField(StandardField.DATE, publishedDateOnly);
            entry.setField(StandardField.YEAR, publishedDateOnly.split("-")[0]);

            if (scholarJsonEntry.has("id")) {
                entry.setField(new UnknownField("scholarapi-id"), scholarJsonEntry.getString("id"));
            }
            // doi
            if (scholarJsonEntry.has("doi")) {
                entry.setField(StandardField.DOI, scholarJsonEntry.getString("doi"));
            }
            // Journal issue
            if (scholarJsonEntry.has("journal_issue")) {
                entry.setField(StandardField.NUMBER, scholarJsonEntry.getString("journal_issue"));
            }
            // Journal pages
            if (scholarJsonEntry.has("journal_pages")) {
                entry.setField(StandardField.PAGES, scholarJsonEntry.getString("journal_pages"));
            }
            // ISSN
            Optional.ofNullable(scholarJsonEntry.optJSONArray("journal_issn")).filter(arr -> !arr.isEmpty()).ifPresent(arr -> entry.setField(StandardField.ISSN, arr.getString(0)));
            // Journal
            if (scholarJsonEntry.has("journal")) {
                entry.setField(StandardField.JOURNAL, scholarJsonEntry.getString("journal"));
            }
            // Url
            if (scholarJsonEntry.has("url")) {
                entry.setField(StandardField.URL, scholarJsonEntry.getString("url"));
            }
            // Abstract
            if (scholarJsonEntry.has("abstract")) {
                entry.setField(StandardField.ABSTRACT, scholarJsonEntry.getString("abstract"));
            }
            // Journal publisher
            if (scholarJsonEntry.has("journal_publisher")) {
                entry.setField(StandardField.PUBLISHER, scholarJsonEntry.getString("journal_publisher"));
            }
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("ScholarAPI JSON format has changed", exception);
        }
    }

    @Override
    public Page<BibEntry> performSearchPaged(BaseQueryNode queryNode, int pageNumber) throws FetcherException {
        ScholarApiQueryTransformer transformer = new ScholarApiQueryTransformer();
        String transformedQuery = transformer.transformSearchQuery(queryNode).orElse("");
        return fetchPage(transformedQuery, pageNumber, transformer.getStartYear(), transformer.getEndYear());
    }

    @Override
    public Page<BibEntry> performRawSearchQueryPaged(String rawQuery, int pageNumber) throws FetcherException {
        if (rawQuery.isBlank()) {
            return new Page<>(rawQuery, pageNumber, List.of());
        }
        return fetchPage(rawQuery, pageNumber, Optional.empty(), Optional.empty());
    }

    private Page<BibEntry> fetchPage(String query, int pageNumber, Optional<Integer> startYear, Optional<Integer> endYear) throws FetcherException {
        URL url;
        try {
            url = buildSearchUrl(query, pageNumber, startYear, endYear);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Invalid URL", e);
        }

        JSONObject response = callListApi(url);

        try {
            int count = response.optInt("count", 0);
            boolean isLastPage = count < getPageSize();

            if (!isLastPage) {
                String nextIndexedAfter = response.getString("next_indexed_after");
                cursorCacheMap.put(new PageKey(query, startYear, endYear, pageNumber + 1), nextIndexedAfter);
            }

            JSONArray results = response.optJSONArray("results");
            List<BibEntry> entries = new ArrayList<>();
            if (results != null) {
                for (int i = 0; i < results.length(); i++) {
                    entries.add(jsonItemToBibEntry(results.getJSONObject(i)));
                }
            }
            return new Page<>(query, pageNumber, entries);
        } catch (JSONException e) {
            throw new FetcherException(url, "ScholarAPI response was not in the expected format", e);
        } catch (ParseException e) {
            throw new FetcherException(url, "ScholarAPI response could not be parsed", e);
        }
    }

    private JSONObject callListApi(URL url) throws FetcherException {
        URLDownload urlDownload = new URLDownload(url);
        importerPreferences.getApiKey(getName()).ifPresent(key -> urlDownload.addHeader("X-API-Key", key));

        try (InputStream stream = urlDownload.asInputStream()) {
            return JsonReader.toJsonObject(stream);
        } catch (IOException | ParseException e) {
            throw new FetcherException(url, "ScholarAPI request failed", e);
        }
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    private URL buildSearchUrl(String query, int pageNumber, Optional<Integer> startYear, Optional<Integer> endYear)
            throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(LIST_URL);
        if (StringUtil.isNotBlank(query)) {
            uriBuilder.setParameter("q", query);
        }
        uriBuilder.setParameter("limit", String.valueOf(getPageSize()));
        startYear.ifPresent(year -> uriBuilder.addParameter("published_after", year + "-01-01"));
        endYear.ifPresent(year -> uriBuilder.addParameter("published_before", (year + 1) + "-01-01"));

        if (pageNumber > 0) {
            String cursor = cursorCacheMap.get(new PageKey(query, startYear, endYear, pageNumber));
            if (cursor == null) {
                throw new URISyntaxException(LIST_URL,
                        "Page " + pageNumber + " was requested before its cursor was available; pages must be fetched sequentially");
            }
            uriBuilder.addParameter("indexed_after", cursor);
        }
        return uriBuilder.build().toURL();
    }

    private record PageKey(String query, Optional<Integer> startYear, Optional<Integer> endYear, int pageNumber) {
    }
}
