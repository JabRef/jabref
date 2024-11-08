package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at <a href="https://www.sciencedirect.com/">ScienceDirect</a>.
 * See <a href="https://dev.elsevier.com/">https://dev.elsevier.com/</a>.
 */
public class ScienceDirect implements FulltextFetcher, CustomizableKeyFetcher {
    public static final String FETCHER_NAME = "ScienceDirect";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScienceDirect.class);

    private static final String API_URL = "https://api.elsevier.com/content/article/doi/";

    private final ImporterPreferences importerPreferences;

    public ScienceDirect(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

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
            return Optional.of(URI.create(link).toURL());
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
            URL url = URI.create(urlFromDoi).toURL();
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
            return Optional.of(URI.create(fullLinkToPdf).toURL());
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

    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}
