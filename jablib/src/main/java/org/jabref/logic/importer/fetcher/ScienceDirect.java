package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher and SearchBasedFetcher implementation for ScienceDirect.
 * Supports both PDF fetching and bibliographic search via Elsevier API.
 */
public class ScienceDirect implements FulltextFetcher, SearchBasedFetcher {
    public static final String FETCHER_NAME = "ScienceDirect";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScienceDirect.class);
    private static final String SEARCH_URL = "https://api.elsevier.com/content/search/sciencedirect";
    private static final String API_URL = "https://api.elsevier.com/content/article/doi/";

    private final ImporterPreferences importerPreferences;

    public ScienceDirect(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_SCIENCE_DIRECT);
    }

    // ========== SearchBasedFetcher Implementation ==========

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // Get API key from preferences
        Optional<String> apiKey = importerPreferences.getApiKey(getName());

        if (apiKey.isEmpty()) {
            LOGGER.warn("No API key configured for ScienceDirect. Please add it in Preferences -> Web search");
            throw new FetcherException("ScienceDirect API key not configured. Please add it in Preferences -> Web search");
        }

        try {
            // Build search URL with query parameter
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrlWithQuery = SEARCH_URL + "?query=" + encodedQuery;

            // Create URL connection with API key
            URLDownload download = new URLDownload(searchUrlWithQuery);
            download.addHeader("X-ELS-APIKey", apiKey.get());
            download.addHeader("Accept", "application/json");

            // Download and parse response
            String response = download.asString();

            // Parse JSON response and convert to BibEntry list
            return parseSearchResponse(response);

        } catch (IOException e) {
            throw new FetcherException("Error searching ScienceDirect", e);
        }
    }

    /**
     * Parse the JSON response from ScienceDirect API and convert to BibEntry objects
     */
    private List<BibEntry> parseSearchResponse(String jsonResponse) throws FetcherException {
        List<BibEntry> entries = new ArrayList<>();

        try {
            JSONObject root = new JSONObject(jsonResponse);

            // Check if we have search results
            if (!root.has("search-results")) {
                return entries;
            }

            JSONObject searchResults = root.getJSONObject("search-results");

            if (!searchResults.has("entry")) {
                return entries;
            }

            Object entryObj = searchResults.get("entry");
            JSONArray entryArray;

            // Handle both single object and array responses
            if (entryObj instanceof JSONArray) {
                entryArray = (JSONArray) entryObj;
            } else {
                entryArray = new JSONArray();
                entryArray.put(entryObj);
            }

            // Parse each entry
            for (int i = 0; i < entryArray.length(); i++) {
                JSONObject item = entryArray.getJSONObject(i);
                BibEntry entry = parseSingleEntry(item);
                if (entry != null) {
                    entries.add(entry);
                }
            }

        } catch (JSONException e) {
            LOGGER.error("Error parsing ScienceDirect response", e);
            throw new FetcherException("Error parsing response from ScienceDirect", e);
        }

        return entries;
    }

    /**
     * Parse a single entry from the JSON response
     */
    private BibEntry parseSingleEntry(JSONObject item) {
        try {
            BibEntry entry = new BibEntry();

            // Set entry type
            String pubType = item.optString("pubType", "");
            if (pubType.toLowerCase().contains("journal")) {
                entry.setType(StandardEntryType.Article);
            } else if (pubType.toLowerCase().contains("book")) {
                entry.setType(StandardEntryType.Book);
            } else {
                entry.setType(StandardEntryType.Misc);
            }

            // Title
            if (item.has("dc:title")) {
                entry.setField(StandardField.TITLE, item.getString("dc:title"));
            }

            // Authors
            if (item.has("authors")) {
                JSONObject authors = item.getJSONObject("authors");
                if (authors.has("author")) {
                    Object authorObj = authors.get("author");
                    StringBuilder authorString = new StringBuilder();

                    if (authorObj instanceof JSONArray) {
                        JSONArray authorArray = (JSONArray) authorObj;
                        for (int j = 0; j < authorArray.length(); j++) {
                            JSONObject author = authorArray.getJSONObject(j);
                            if (author.has("$")) {
                                if (authorString.length() > 0) {
                                    authorString.append(" and ");
                                }
                                authorString.append(author.getString("$"));
                            }
                        }
                    } else if (authorObj instanceof JSONObject) {
                        JSONObject author = (JSONObject) authorObj;
                        if (author.has("$")) {
                            authorString.append(author.getString("$"));
                        }
                    }

                    if (authorString.length() > 0) {
                        entry.setField(StandardField.AUTHOR, authorString.toString());
                    }
                }
            }

            // DOI
            if (item.has("prism:doi")) {
                entry.setField(StandardField.DOI, item.getString("prism:doi"));
            }

            // Publication name (journal/book)
            if (item.has("prism:publicationName")) {
                String pubName = item.getString("prism:publicationName");
                if (entry.getType().equals(StandardEntryType.Article)) {
                    entry.setField(StandardField.JOURNAL, pubName);
                } else if (entry.getType().equals(StandardEntryType.Book)) {
                    entry.setField(StandardField.PUBLISHER, pubName);
                }
            }

            // Year
            if (item.has("prism:coverDate")) {
                String coverDate = item.getString("prism:coverDate");
                // Extract year from date string (format: YYYY-MM-DD)
                if (coverDate.length() >= 4) {
                    entry.setField(StandardField.YEAR, coverDate.substring(0, 4));
                }
            }

            // Volume
            if (item.has("prism:volume")) {
                entry.setField(StandardField.VOLUME, item.getString("prism:volume"));
            }

            // Pages
            if (item.has("prism:pageRange")) {
                entry.setField(StandardField.PAGES, item.getString("prism:pageRange"));
            }

            // URL
            if (item.has("link")) {
                Object linkObj = item.get("link");
                if (linkObj instanceof JSONArray) {
                    JSONArray linkArray = (JSONArray) linkObj;
                    for (int j = 0; j < linkArray.length(); j++) {
                        JSONObject link = linkArray.getJSONObject(j);
                        if (link.has("@ref") && "scidir".equals(link.getString("@ref"))) {
                            if (link.has("@href")) {
                                entry.setField(StandardField.URL, link.getString("@href"));
                                break;
                            }
                        }
                    }
                }
            }

            // Abstract
            if (item.has("dc:description")) {
                entry.setField(StandardField.ABSTRACT, item.getString("dc:description"));
            }

            return entry;

        } catch (JSONException e) {
            LOGGER.warn("Could not parse entry from ScienceDirect", e);
            return null;
        }
    }

    // ========== FulltextFetcher Implementation ==========

    @Override
    public Optional<URL> findFullText(@NonNull BibEntry entry) throws IOException {
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isEmpty()) {
            // Full text fetching works only if a DOI is present
            return Optional.empty();
        }

        String urlFromDoi = getUrlByDoi(doi.get().getDOI());
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
            String request = API_URL + doi;
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
}
