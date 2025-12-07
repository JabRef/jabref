package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.paging.Page;
import org.jabref.model.search.query.BaseQueryNode;
import org.jabref.model.search.query.SearchQueryNode;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher and SearchBasedFetcher implementation for <a href="https://www.sciencedirect.com/">ScienceDirect</a>.
 * Uses ScienceDirect Search API v2 with PUT method (recommended).
 * See <a href="https://dev.elsevier.com/">https://dev.elsevier.com/</a>.
 */
public class ScienceDirect implements FulltextFetcher, CustomizableKeyFetcher, PagedSearchBasedFetcher {
    public static final String FETCHER_NAME = "ScienceDirect";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScienceDirect.class);

    private static final String ARTICLE_API_URL = "https://api.elsevier.com/content/article/doi/";
    private static final String SEARCH_API_URL = "https://api.elsevier.com/content/search/sciencedirect";

    // Valid page sizes for API v2: 10, 25, 50, 100
    private static final int DEFAULT_PAGE_SIZE = 25;
    // Maximum offset allowed by API v2
    private static final int MAX_OFFSET = 6000;

    private final ImporterPreferences importerPreferences;

    public ScienceDirect(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public Optional<URL> findFullText(@NonNull BibEntry entry) throws IOException {
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isEmpty()) {
            // Full text fetching works only if a DOI is present
            return Optional.empty();
        }

        String urlFromDoi = getUrlByDoi(doi.get().asString());
        if (urlFromDoi.isEmpty()) {
            return Optional.empty();
        }
        // Scrape the web page as desktop client (not as mobile client!)
        Document html = Jsoup.connect(urlFromDoi)
                             .userAgent(URLDownload.USER_AGENT)
                             .referrer("https://www.google.com")
                             .ignoreHttpErrors(true)
                             .get();

        // Retrieve PDF link from meta data (most recent)
        Elements metaLinks = html.getElementsByAttributeValue("name", "citation_pdf_url");
        if (!metaLinks.isEmpty()) {
            String link = metaLinks.first().attr("content");
            return Optional.of(URLUtil.create(link));
        }

        // We use the ScienceDirect web page which contains the article (presented using HTML).
        // This page contains the link to the PDF in some JavaScript code embedded in the web page.
        // Example page: https://www.sciencedirect.com/science/article/pii/S1674775515001079

        Optional<JSONObject> pdfDownloadOptional = html
                .getElementsByAttributeValue("type", "application/json")
                .stream()
                .flatMap(element -> element.getElementsByTag("script").stream())
                // The first DOM child of the script element is the script itself (represented as HTML text)
                .map(element -> element.childNode(0))
                .map(Node::toString)
                .map(JSONObject::new)
                .filter(json -> json.has("article"))
                .map(json -> json.getJSONObject("article"))
                .filter(json -> json.has("pdfDownload"))
                .map(json -> json.getJSONObject("pdfDownload"))
                .findAny();

        if (pdfDownloadOptional.isEmpty()) {
            LOGGER.debug("No 'pdfDownload' key found in JSON information");
            return Optional.empty();
        }

        JSONObject pdfDownload = pdfDownloadOptional.get();

        String fullLinkToPdf;
        if (pdfDownload.has("linkToPdf")) {
            String linkToPdf = pdfDownload.getString("linkToPdf");
            URL url = URLUtil.create(urlFromDoi);
            fullLinkToPdf = "%s://%s%s".formatted(url.getProtocol(), url.getAuthority(), linkToPdf);
        } else if (pdfDownload.has("urlMetadata")) {
            JSONObject urlMetadata = pdfDownload.getJSONObject("urlMetadata");
            JSONObject queryParamsObject = urlMetadata.getJSONObject("queryParams");
            String queryParameters = queryParamsObject.keySet().stream()
                                                      .map(key -> "%s=%s".formatted(key, queryParamsObject.getString(key)))
                                                      .collect(Collectors.joining("&"));
            fullLinkToPdf = "https://www.sciencedirect.com/%s/%s%s?%s".formatted(
                    urlMetadata.getString("path"),
                    urlMetadata.getString("pii"),
                    urlMetadata.getString("pdfExtension"),
                    queryParameters);
        } else {
            LOGGER.debug("No suitable data in JSON information");
            return Optional.empty();
        }

        LOGGER.info("Fulltext PDF found at ScienceDirect at {}.", fullLinkToPdf);
        try {
            return Optional.of(URLUtil.create(fullLinkToPdf));
        } catch (MalformedURLException e) {
            LOGGER.error("malformed URL", e);
            return Optional.empty();
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    private String getUrlByDoi(String doi) throws UnirestException {
        String sciLink = "";
        try {
            String request = ARTICLE_API_URL + doi;
            HttpResponse<JsonNode> jsonResponse = Unirest.get(request)
                                                         .header("X-ELS-APIKey", importerPreferences.getApiKey(getName()).orElse(""))
                                                         .queryString("httpAccept", "application/json")
                                                         .asJson();

            JSONObject json = jsonResponse.getBody().getObject();
            JSONArray links = json.getJSONObject("full-text-retrieval-response")
                                  .getJSONObject("coredata")
                                  .getJSONArray("link");

            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.getJSONObject(i);
                if ("scidir".equals(link.getString("@rel"))) {
                    sciLink = link.getString("@href");
                }
            }
            return sciLink;
        } catch (JSONException e) {
            LOGGER.debug("No ScienceDirect link found in API request", e);
            return sciLink;
        }
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public String getTestUrl() {
        return SEARCH_API_URL + "?query=test&count=1&apiKey=";
    }

    @Override
    public int getPageSize() {
        return DEFAULT_PAGE_SIZE;
    }

    /**
     * Performs a search using the ScienceDirect Search API v2 with the PUT method.
     *
     * @param searchQuery the search query
     * @param pageNumber the page number (0-indexed)
     * @return Page containing the search results
     * @throws FetcherException if the search fails
     */
    @Override
    public Page<BibEntry> performSearchPaged(BaseQueryNode searchQuery, int pageNumber) throws FetcherException {
        Optional<String> apiKey = importerPreferences.getApiKey(getName());
        if (apiKey.isEmpty() || apiKey.get().isBlank()) {
            throw new FetcherException("ScienceDirect API key is required. Please configure it in Preferences → Web search.");
        }

        int offset = pageNumber * getPageSize();
        if (offset > MAX_OFFSET) {
            LOGGER.warn("Requested offset {} exceeds maximum allowed offset {}. Returning empty results.", offset, MAX_OFFSET);
            return new Page<>(searchQuery.toString(), pageNumber, List.of());
        }

        try {
            // Build the request body JSON for API v2 PUT method
            JSONObject requestBody = buildRequestBody(searchQuery, offset);

            LOGGER.debug("ScienceDirect API v2 request body: {}", requestBody);

            HttpResponse<JsonNode> response = Unirest.put(SEARCH_API_URL)
                                                     .header("X-ELS-APIKey", apiKey.get())
                                                     .header("Content-Type", "application/json")
                                                     .header("Accept", "application/json")
                                                     .body(requestBody.toString())
                                                     .asJson();

            if (response.getStatus() == 401) {
                throw new FetcherException("Invalid API key. Please check your ScienceDirect API key in Preferences → Web search.");
            }

            if (response.getStatus() == 403) {
                throw new FetcherException("Access denied. Your API key does not have access to ScienceDirect Search API. " +
                        "This API requires institutional subscription or special authorization from Elsevier.");
            }

            if (response.getStatus() == 400) {
                String errorMessage = "Bad request";
                if (response.getBody() != null && response.getBody().getObject().has("message")) {
                    errorMessage = response.getBody().getObject().getString("message");
                }
                throw new FetcherException("ScienceDirect API error: " + errorMessage);
            }

            // Check for service-error in response body (can occur with 200 status)
            if (response.getBody() != null && response.getBody().getObject().has("service-error")) {
                JSONObject serviceError = response.getBody().getObject().getJSONObject("service-error");
                if (serviceError.has("status")) {
                    JSONObject status = serviceError.getJSONObject("status");
                    String statusCode = status.optString("statusCode", "UNKNOWN");
                    String statusText = status.optString("statusText", "Unknown error");

                    if ("AUTHORIZATION_ERROR".equals(statusCode)) {
                        throw new FetcherException("Access denied: " + statusText + ". " +
                                "ScienceDirect Search API requires institutional subscription or authorization from Elsevier.");
                    }
                    throw new FetcherException("ScienceDirect API error: " + statusCode + " - " + statusText);
                }
            }

            if (response.getStatus() != 200) {
                throw new FetcherException("ScienceDirect API returned status " + response.getStatus());
            }

            List<BibEntry> entries = parseSearchResponse(response.getBody().getObject());
            return new Page<>(searchQuery.toString(), pageNumber, entries);
        } catch (UnirestException e) {
            throw new FetcherException("Error connecting to ScienceDirect API", e);
        }
    }

    /**
     * Builds the JSON request body for ScienceDirect Search API v2.
     *
     * @param searchQuery the search query
     * @param offset the starting position for results
     * @return JSONObject representing the request body
     */
    private JSONObject buildRequestBody(BaseQueryNode searchQuery, int offset) {
        JSONObject requestBody = new JSONObject();

        // Extract query terms and build appropriate fields
        String queryString = extractQueryString(searchQuery);

        // For general searches, use the "qs" (quick search) field
        // This searches over all article/book chapter content excluding references
        requestBody.put("qs", queryString);

        // Add display parameters for pagination and sorting
        JSONObject display = new JSONObject();
        display.put("offset", offset);
        display.put("show", getPageSize());
        display.put("sortBy", "relevance");
        requestBody.put("display", display);

        return requestBody;
    }

    /**
     * Extracts a plain query string from the search query node.
     * For API v2, we use a simpler approach as the API handles boolean operators.
     *
     * @param searchQuery the search query node
     * @return the query string
     */
    private String extractQueryString(BaseQueryNode searchQuery) {
        if (searchQuery instanceof SearchQueryNode sqn) {
            return sqn.term();
        }
        // For complex queries, return the string representation
        // The API v2 supports AND, OR, NOT operators in the query string
        return searchQuery.toString();
    }

    /**
     * Parses the API v2 response and extracts BibEntry objects.
     *
     * @param jsonResponse the JSON response from the API
     * @return list of BibEntry objects
     */
    private List<BibEntry> parseSearchResponse(JSONObject jsonResponse) {
        List<BibEntry> entries = new ArrayList<>();

        if (!jsonResponse.has("results")) {
            if (jsonResponse.has("message")) {
                LOGGER.warn("ScienceDirect API message: {}", jsonResponse.getString("message"));
            }
            return entries;
        }

        JSONArray results = jsonResponse.getJSONArray("results");
        int resultsFound = jsonResponse.optInt("resultsFound", 0);
        LOGGER.debug("ScienceDirect returned {} total results", resultsFound);

        for (int i = 0; i < results.length(); i++) {
            try {
                JSONObject jsonEntry = results.getJSONObject(i);
                BibEntry entry = parseJsonEntry(jsonEntry);
                if (entry != null) {
                    entries.add(entry);
                }
            } catch (JSONException e) {
                LOGGER.warn("Error parsing ScienceDirect entry at index {}", i, e);
            }
        }

        return entries;
    }

    /**
     * Parses a single JSON entry from the API v2 response to a BibEntry.
     *
     * @param jsonEntry JSON object representing a single search result
     * @return BibEntry or null if parsing fails
     */
    private BibEntry parseJsonEntry(JSONObject jsonEntry) {
        try {
            BibEntry entry = new BibEntry();

            // Default to Article type (ScienceDirect primarily contains journal articles)
            entry.setType(StandardEntryType.Article);

            // Title
            if (jsonEntry.has("title")) {
                entry.setField(StandardField.TITLE, jsonEntry.getString("title"));
            }

            // Authors - API v2 returns array with {order, name} objects
            if (jsonEntry.has("authors")) {
                JSONArray authors = jsonEntry.getJSONArray("authors");
                List<String> authorNames = new ArrayList<>();
                for (int j = 0; j < authors.length(); j++) {
                    JSONObject author = authors.getJSONObject(j);
                    if (author.has("name")) {
                        authorNames.add(author.getString("name"));
                    }
                }
                if (!authorNames.isEmpty()) {
                    entry.setField(StandardField.AUTHOR, String.join(" and ", authorNames));
                }
            }

            // DOI
            if (jsonEntry.has("doi")) {
                entry.setField(StandardField.DOI, jsonEntry.getString("doi"));
            }

            // Source title (journal/book name)
            if (jsonEntry.has("sourceTitle")) {
                entry.setField(StandardField.JOURNAL, jsonEntry.getString("sourceTitle"));
            }

            // Publication date - format: "2018-07-17"
            if (jsonEntry.has("publicationDate")) {
                String pubDate = jsonEntry.getString("publicationDate");
                if (pubDate.contains("-")) {
                    String year = pubDate.split("-")[0];
                    entry.setField(StandardField.YEAR, year);

                    // Also extract month if available
                    String[] parts = pubDate.split("-");
                    if (parts.length >= 2) {
                        entry.setField(StandardField.MONTH, parts[1]);
                    }
                }
            }

            // Volume and Issue - API v2 combines them in "volumeIssue" field
            // Format examples: "Volume 98", "Volume 4, Issue 7"
            if (jsonEntry.has("volumeIssue")) {
                String volumeIssue = jsonEntry.getString("volumeIssue");
                parseVolumeIssue(volumeIssue, entry);
            }

            // Pages - API v2 returns object with "first" and "last" fields
            if (jsonEntry.has("pages")) {
                JSONObject pages = jsonEntry.getJSONObject("pages");
                String first = pages.optString("first", "");
                String last = pages.optString("last", "");
                if (!first.isEmpty()) {
                    String pageStr = first;
                    if (!last.isEmpty() && !last.equals(first)) {
                        pageStr += "--" + last;
                    }
                    entry.setField(StandardField.PAGES, pageStr);
                }
            }

            // URL - use the uri field from API v2
            if (jsonEntry.has("uri")) {
                entry.setField(StandardField.URL, jsonEntry.getString("uri"));
            }

            // PII (Publisher Item Identifier) - ScienceDirect specific
            if (jsonEntry.has("pii")) {
                entry.setField(StandardField.EPRINT, jsonEntry.getString("pii"));
            }

            // Open Access flag - store as note
            if (jsonEntry.has("openAccess") && jsonEntry.getBoolean("openAccess")) {
                entry.setField(StandardField.NOTE, "Open Access");
            }

            return entry;
        } catch (JSONException e) {
            LOGGER.warn("Error parsing ScienceDirect entry", e);
            return null;
        }
    }

    /**
     * Parses the volumeIssue string from API v2 response.
     * Examples: "Volume 98", "Volume 4, Issue 7"
     *
     * @param volumeIssue the combined volume/issue string
     * @param entry the BibEntry to populate
     */
    private void parseVolumeIssue(String volumeIssue, BibEntry entry) {
        if (volumeIssue == null || volumeIssue.isEmpty()) {
            return;
        }

        // Try to extract volume
        if (volumeIssue.toLowerCase().contains("volume")) {
            String[] parts = volumeIssue.split(",");
            for (String part : parts) {
                part = part.trim();
                if (part.toLowerCase().startsWith("volume")) {
                    String volume = part.replaceFirst("(?i)volume\\s*", "").trim();
                    if (!volume.isEmpty()) {
                        entry.setField(StandardField.VOLUME, volume);
                    }
                } else if (part.toLowerCase().startsWith("issue")) {
                    String issue = part.replaceFirst("(?i)issue\\s*", "").trim();
                    if (!issue.isEmpty()) {
                        entry.setField(StandardField.NUMBER, issue);
                    }
                }
            }
        } else {
            // Fallback: treat as just a volume number
            entry.setField(StandardField.VOLUME, volumeIssue);
        }
    }
}
