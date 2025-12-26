package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.ScopusQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SearchBasedFetcher implementation using <a href="https://dev.elsevier.com/">Elsevier's Scopus Search API</a>.
 * API Documentation: <a href="https://dev.elsevier.com/documentation/ScopusSearchAPI.wadl">Scopus Search API</a>
 */
@NullMarked
public class Scopus implements PagedSearchBasedParserFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "Scopus";

    private static final Logger LOGGER = LoggerFactory.getLogger(Scopus.class);

    private static final String API_URL = "https://api.elsevier.com/content/search/scopus";

    private final ImporterPreferences importerPreferences;

    @Nullable
    private ScopusQueryTransformer transformer;

    public Scopus(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public String getTestUrl() {
        return API_URL + "?query=test&count=1&apiKey=";
    }

    /**
     * Constructs a URL for querying the Scopus Search API.
     *
     * @param queryNode  the search query node
     * @param pageNumber the page number (0-indexed)
     * @return URL for the Scopus API request
     */
    @Override
    public URL getURLForQuery(BaseQueryNode queryNode, int pageNumber) throws URISyntaxException, MalformedURLException {
        transformer = new ScopusQueryTransformer();
        String transformedQuery = transformer.transformSearchQuery(queryNode).orElse("");

        URIBuilder uriBuilder = new URIBuilder(API_URL);
        importerPreferences.getApiKey(FETCHER_NAME).ifPresent(apiKey -> uriBuilder.addParameter("apiKey", apiKey));

        if (!transformedQuery.isBlank()) {
            uriBuilder.addParameter("query", transformedQuery);
        }

        uriBuilder.addParameter("count", String.valueOf(getPageSize()));
        uriBuilder.addParameter("start", String.valueOf(pageNumber * getPageSize()));
        uriBuilder.addParameter("view", "STANDARD");
        uriBuilder.addParameter("suppressNavLinks", "true");
        uriBuilder.addParameter("sort", "relevancy");

        LOGGER.debug("Scopus Search URL: {}", uriBuilder.build().toString());

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject jsonObject = JsonReader.toJsonObject(inputStream);
            List<BibEntry> entries = new ArrayList<>();

            if (jsonObject.has("service-error")) {
                JSONObject serviceError = jsonObject.getJSONObject("service-error");
                if (serviceError.has("status")) {
                    JSONObject status = serviceError.getJSONObject("status");
                    String statusCode = status.optString("statusCode", "UNKNOWN");
                    String statusText = status.optString("statusText", "Unknown error");
                    LOGGER.error("Scopus API error: {} - {}", statusCode, statusText);
                }
                return entries;
            }

            if (!jsonObject.has("search-results")) {
                LOGGER.warn("No search-results in Scopus response");
                return entries;
            }

            JSONObject searchResults = jsonObject.getJSONObject("search-results");

            String totalResults = searchResults.optString("opensearch:totalResults", "0");
            LOGGER.debug("Scopus returned {} total results", totalResults);

            if (!searchResults.has("entry")) {
                return entries;
            }

            JSONArray resultsArray = searchResults.getJSONArray("entry");

            for (int i = 0; i < resultsArray.length(); i++) {
                try {
                    JSONObject jsonEntry = resultsArray.getJSONObject(i);

                    if (jsonEntry.has("error")) {
                        LOGGER.debug("Scopus entry error: {}", jsonEntry.optString("error"));
                        continue;
                    }

                    BibEntry entry = parseScopusEntry(jsonEntry);
                    if (entry != null) {
                        if (shouldIncludeEntry(entry)) {
                            entries.add(entry);
                        }
                    }
                } catch (JSONException e) {
                    LOGGER.warn("Error parsing Scopus entry at index {}", i, e);
                }
            }

            return entries;
        };
    }

    /**
     * Checks if the entry should be included based on year filtering.
     */
    private boolean shouldIncludeEntry(BibEntry entry) {
        if (transformer == null) {
            return true;
        }

        Optional<Integer> startYear = transformer.getStartYear();
        Optional<Integer> endYear = transformer.getEndYear();

        if (startYear.isEmpty() && endYear.isEmpty()) {
            return true;
        }

        Optional<String> yearField = entry.getField(StandardField.YEAR);
        if (yearField.isEmpty()) {
            return true;
        }

        try {
            int year = Integer.parseInt(yearField.get());
            boolean afterStart = startYear.map(start -> year >= start).orElse(true);
            boolean beforeEnd = endYear.map(end -> year <= end).orElse(true);
            return afterStart && beforeEnd;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /**
     * Parses a single Scopus JSON entry into a BibEntry.
     *
     * @param jsonEntry the JSON object representing a Scopus search result
     * @return BibEntry or null if parsing fails
     */
    @Nullable
    private BibEntry parseScopusEntry(JSONObject jsonEntry) {
        try {
            BibEntry entry = new BibEntry();

            // Determine entry type based on aggregationType and subtype
            String aggregationType = jsonEntry.optString("prism:aggregationType", "");
            String subtype = jsonEntry.optString("subtype", "");
            entry.setType(determineEntryType(aggregationType, subtype));

            // Title (dc:title)
            String title = jsonEntry.optString("dc:title", "");
            if (!title.isEmpty()) {
                entry.setField(StandardField.TITLE, title);
            }

            // Author (dc:creator for first author, or parse author array)
            String creator = jsonEntry.optString("dc:creator", "");
            if (!creator.isEmpty()) {
                entry.setField(StandardField.AUTHOR, creator);
            }
            // Note: For COMPLETE view, full author list might be in "author" array
            if (jsonEntry.has("author")) {
                JSONArray authors = jsonEntry.getJSONArray("author");
                List<String> authorNames = new ArrayList<>();
                for (int j = 0; j < authors.length(); j++) {
                    JSONObject author = authors.getJSONObject(j);
                    String authorName = author.optString("authname", author.optString("given-name", "") + " " + author.optString("surname", "")).trim();
                    if (!authorName.isEmpty()) {
                        authorNames.add(authorName);
                    }
                }
                if (!authorNames.isEmpty()) {
                    entry.setField(StandardField.AUTHOR, String.join(" and ", authorNames));
                }
            }

            // DOI (prism:doi)
            String doi = jsonEntry.optString("prism:doi", "");
            if (!doi.isEmpty()) {
                entry.setField(StandardField.DOI, doi);
            }

            // Journal/Source title (prism:publicationName)
            String journal = jsonEntry.optString("prism:publicationName", "");
            if (!journal.isEmpty()) {
                entry.setField(StandardField.JOURNAL, journal);
            }

            // Volume (prism:volume)
            String volume = jsonEntry.optString("prism:volume", "");
            if (!volume.isEmpty()) {
                entry.setField(StandardField.VOLUME, volume);
            }

            // Issue (prism:issueIdentifier)
            String issue = jsonEntry.optString("prism:issueIdentifier", "");
            if (!issue.isEmpty()) {
                entry.setField(StandardField.NUMBER, issue);
            }

            // Pages (prism:pageRange)
            String pageRange = jsonEntry.optString("prism:pageRange", "");
            if (!pageRange.isEmpty()) {
                // Convert hyphen to double dash for BibTeX
                entry.setField(StandardField.PAGES, pageRange.replace("-", "--"));
            }

            // Year from coverDate (prism:coverDate format: YYYY-MM-DD)
            String coverDate = jsonEntry.optString("prism:coverDate", "");
            if (coverDate.length() >= 4) {
                entry.setField(StandardField.YEAR, coverDate.substring(0, 4));
                // Extract month if available
                if (coverDate.length() >= 7) {
                    entry.setField(StandardField.MONTH, coverDate.substring(5, 7));
                }
            }

            // URL - use Scopus link or DOI link
            if (jsonEntry.has("link")) {
                JSONArray links = jsonEntry.getJSONArray("link");
                for (int i = 0; i < links.length(); i++) {
                    JSONObject link = links.getJSONObject(i);
                    String ref = link.optString("@ref", "");
                    if ("scopus".equals(ref)) {
                        entry.setField(StandardField.URL, link.optString("@href", ""));
                        break;
                    }
                }
            }

            // Abstract (dc:description) - available in COMPLETE view
            String description = jsonEntry.optString("dc:description", "");
            if (!description.isEmpty()) {
                entry.setField(StandardField.ABSTRACT, description);
            }

            // ISSN (prism:issn)
            String issn = jsonEntry.optString("prism:issn", "");
            if (!issn.isEmpty()) {
                entry.setField(StandardField.ISSN, issn);
            }

            // eISSN (prism:eIssn)
            String eissn = jsonEntry.optString("prism:eIssn", "");
            if (!eissn.isEmpty() && !entry.hasField(StandardField.ISSN)) {
                // Only set if ISSN not already set
                entry.setField(StandardField.ISSN, eissn);
            }

            // Keywords (authkeywords) - available in COMPLETE view
            String keywords = jsonEntry.optString("authkeywords", "");
            if (!keywords.isEmpty()) {
                entry.setField(StandardField.KEYWORDS, keywords);
            }

            // Open access flag
            if (jsonEntry.has("openaccessFlag")) {
                try {
                    if (jsonEntry.getBoolean("openaccessFlag")) {
                        entry.setField(StandardField.NOTE, "Open Access");
                    }
                } catch (JSONException e) {
                    // openaccessFlag might be null or not a boolean
                    LOGGER.debug("Could not parse openaccessFlag", e);
                }
            }

            // Scopus EID (unique identifier)
            String eid = jsonEntry.optString("eid", "");
            if (!eid.isEmpty()) {
                entry.setField(StandardField.EPRINT, eid);
            }

            return entry;
        } catch (JSONException e) {
            LOGGER.warn("Error parsing Scopus entry", e);
            return null;
        }
    }

    /**
     * Determines the BibTeX entry type based on Scopus aggregationType and subtype.
     *
     * @param aggregationType the aggregation type (Journal, Book, Conference Proceeding, etc.)
     * @param subtype the document subtype (ar, cp, re, etc.)
     * @return appropriate StandardEntryType
     */
    private StandardEntryType determineEntryType(String aggregationType, String subtype) {
        return switch (subtype.toLowerCase()) {
            case "cp" ->
                    StandardEntryType.InProceedings;
            case "bk" ->
                    StandardEntryType.Book;
            case "ch" ->
                    StandardEntryType.InCollection;
            case "re" ->
                    StandardEntryType.Article;
            case "ed" ->
                    StandardEntryType.Article;
            case "le" ->
                    StandardEntryType.Article;
            case "no" ->
                    StandardEntryType.Misc;
            case "sh" ->
                    StandardEntryType.Article;
            default ->
                    switch (aggregationType.toLowerCase()) {
                        case "book" ->
                                StandardEntryType.Book;
                        case "book series" ->
                                StandardEntryType.InCollection;
                        case "conference proceeding" ->
                                StandardEntryType.InProceedings;
                        case "trade publication" ->
                                StandardEntryType.Article;
                        default ->
                                StandardEntryType.Article; // Default to Article
                    };
        };
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
