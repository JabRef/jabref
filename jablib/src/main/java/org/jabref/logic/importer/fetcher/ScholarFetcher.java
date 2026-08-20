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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.transformers.ScholarApiQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.paging.Page;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class ScholarFetcher implements FulltextFetcher, PagedSearchBasedFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "ScholarAPI";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScholarFetcher.class);

    private static final String LIST_URL = "https://scholarapi.net/api/v1/list";
    private static final String PDF_URL = "https://scholarapi.net/api/v1/pdf";

    private static final int NO_YEAR_BOUND = Integer.MIN_VALUE;

    private static final Pattern JOURNAL_VOLUME = Pattern.compile("Volume\\s+([^,\\s]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern JOURNAL_ISSUE_NUMBER = Pattern.compile("Issue\\s+([^,]+)", Pattern.CASE_INSENSITIVE);

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
                    String rawAuthors = String.join(" and ", authorsList);
                    AuthorList parsedAuthors = AuthorList.parse(rawAuthors);
                    entry.setField(StandardField.AUTHOR, parsedAuthors.getAsFirstLastNamesWithAnd());
                } else {
                    LOGGER.debug("Empty authors array.");
                }
            } else {
                LOGGER.debug("No authors found.");
            }

            entry.setField(StandardField.TITLE, scholarJsonEntry.getString("title"));
            String publishedDate = scholarJsonEntry.getString("published_date");
            String publishedDateOnly = publishedDate.split("T")[0];
            entry.setField(StandardField.DATE, publishedDateOnly);
            entry.setField(StandardField.YEAR, publishedDateOnly.split("-")[0]);

            // ScholarAPI's has_text/has_pdf flags for future fulltext-fetcher integration without needing to re fetch metadata to check availability first
            entry.setField(new UnknownField("scholarApiHasText"), String.valueOf(scholarJsonEntry.getBoolean("has_text")));
            entry.setField(new UnknownField("scholarApiHasPdf"), String.valueOf(scholarJsonEntry.getBoolean("has_pdf")));

            if (scholarJsonEntry.has("id")) {
                entry.setField(new UnknownField("scholarapi"), scholarJsonEntry.getString("id"));
            }

            if (scholarJsonEntry.has("doi")) {
                entry.setField(StandardField.DOI, scholarJsonEntry.getString("doi"));
            }

            if (scholarJsonEntry.has("journal_pages")) {
                entry.setField(StandardField.PAGES, scholarJsonEntry.getString("journal_pages"));
            }

            Optional.ofNullable(scholarJsonEntry.optJSONArray("journal_issn")).filter(arr -> !arr.isEmpty()).ifPresent(arr -> entry.setField(StandardField.ISSN, arr.getString(0)));
            // Journal
            if (scholarJsonEntry.has("journal")) {
                entry.setField(StandardField.JOURNAL, scholarJsonEntry.getString("journal"));
            }

            if (scholarJsonEntry.has("journal_issue")) {
                String journalIssue = scholarJsonEntry.getString("journal_issue");
                Matcher volume = JOURNAL_VOLUME.matcher(journalIssue);
                Matcher issue = JOURNAL_ISSUE_NUMBER.matcher(journalIssue);
                boolean matchedVolume = volume.find();
                boolean matchedIssue = issue.find();

                if (matchedVolume) {
                    entry.setField(StandardField.VOLUME, volume.group(1).trim());
                }
                if (matchedIssue) {
                    entry.setField(StandardField.NUMBER, issue.group(1).trim());
                }
                if (!matchedVolume && !matchedIssue) {
                    entry.setField(StandardField.NUMBER, journalIssue);
                }
            }

            if (scholarJsonEntry.has("url")) {
                entry.setField(StandardField.URL, scholarJsonEntry.getString("url"));
            }

            if (scholarJsonEntry.has("abstract")) {
                entry.setField(StandardField.ABSTRACT, scholarJsonEntry.getString("abstract"));
            }

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
        if (query.isBlank() && startYear.isEmpty() && endYear.isEmpty()) {
            return new Page<>(query, pageNumber, List.of());
        }
        if (pageNumber == 0) {
            int keyStartYear = startYear.orElse(NO_YEAR_BOUND);
            int keyEndYear = endYear.orElse(NO_YEAR_BOUND);
            cursorCacheMap.keySet().removeIf(key ->
                    key.query().equals(query) && key.startYear() == keyStartYear && key.endYear() == keyEndYear);
        }
        URL url;
        try {
            url = buildSearchUrl(query, pageNumber, startYear, endYear);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Invalid URL", e);
        }

        JSONObject response = callListApi(url);

        try {
            JSONArray results = response.optJSONArray("results");
            int resultCount = results == null ? 0 : results.length();
            boolean isLastPage = resultCount < getPageSize();

            if (!isLastPage) {
                Optional.ofNullable(response.optString("next_indexed_after", null))
                        .filter(StringUtil::isNotBlank)
                        .ifPresent(cursor -> cursorCacheMap.put(
                                new PageKey(query, startYear.orElse(NO_YEAR_BOUND), endYear.orElse(NO_YEAR_BOUND), pageNumber + 1),
                                cursor));
            }

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
        importerPreferences.getApiKey(getName())
                           .filter(key -> !key.isBlank())
                           .ifPresent(key -> urlDownload.addHeader("X-API-Key", key));

        try (InputStream stream = urlDownload.asInputStream()) {
            return JsonReader.toJsonObject(stream);
        } catch (IOException | ParseException e) {
            throw new FetcherException(url, "ScholarAPI request failed", e);
        }
    }

    @Override
    public boolean isValidKey(@NonNull String apiKey) {
        try {
            URLDownload urlDownload = new URLDownload(getTestUrl());
            urlDownload.addHeader("X-API-Key", apiKey);
            int statusCode = ((HttpURLConnection) urlDownload.openConnection()).getResponseCode();
            return (statusCode >= 200) && (statusCode < 300);
        } catch (IOException | FetcherException e) {
            return false;
        }
    }

    private URL getTestUrl() throws MalformedURLException {
        return URLUtil.create(LIST_URL + "?limit=1");
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    private URL buildSearchUrl(String query, int pageNumber, Optional<Integer> startYear, Optional<Integer> endYear)
            throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(LIST_URL);
        if (StringUtil.isNotBlank(query)) {
            uriBuilder.setParameter("q", query);
        }
        uriBuilder.setParameter("limit", String.valueOf(getPageSize()));
        startYear.ifPresent(year -> uriBuilder.addParameter("published_after", year + "-01-01"));
        endYear.ifPresent(year -> uriBuilder.addParameter("published_before", (year + 1) + "-01-01"));

        if (pageNumber > 0) {
            String cursor = cursorCacheMap.get(new PageKey(query, startYear.orElse(NO_YEAR_BOUND), endYear.orElse(NO_YEAR_BOUND), pageNumber));
            if (cursor == null) {
                throw new FetcherException(
                        "Page " + pageNumber + " was requested before its cursor was available; pages must be fetched sequentially");
            }
            uriBuilder.addParameter("indexed_after", cursor);
        }
        return uriBuilder.build().toURL();
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Optional<String> id = entry.getField(new UnknownField("scholarapi"));
        if(id.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> hasPdf = entry.getField(new UnknownField("scholarApiHasPdf"));
        if(hasPdf.filter(value -> !Boolean.parseBoolean(value)).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(URLUtil.create(PDF_URL + "/" + id.get()));
    }

    @Override
    public Map<String, String> getDownloadHeaders() {
        return importerPreferences.getApiKey(getName())
                                  .filter(key -> !key.isBlank())
                                  .map(key -> Map.of("X-API-Key", key))
                                  .orElse(Map.of());
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    private record PageKey(String query, int startYear, int endYear, int pageNumber) {
    }
}
