package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
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
public class ScienceDirect implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScienceDirect.class);

    private static final String API_URL = "https://api.elsevier.com/content/article/doi/";
    private static final String API_KEY = new BuildInfo().scienceDirectApiKey;

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

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
            return Optional.of(new URL(link));
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
            URL url = new URL(urlFromDoi);
            fullLinkToPdf = String.format("%s://%s%s", url.getProtocol(), url.getAuthority(), linkToPdf);
        } else if (pdfDownload.has("urlMetadata")) {
            JSONObject urlMetadata = pdfDownload.getJSONObject("urlMetadata");
            JSONObject queryParamsObject = urlMetadata.getJSONObject("queryParams");
            String queryParameters = queryParamsObject.keySet().stream()
                                                      .map(key -> String.format("%s=%s", key, queryParamsObject.getString(key)))
                                                      .collect(Collectors.joining("&"));
            fullLinkToPdf = String.format("https://www.sciencedirect.com/%s/%s%s?%s",
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
            return Optional.of(new URL(fullLinkToPdf));
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
                                                         .header("X-ELS-APIKey", API_KEY)
                                                         .queryString("httpAccept", "application/json")
                                                         .asJson();

            JSONObject json = jsonResponse.getBody().getObject();
            JSONArray links = json.getJSONObject("full-text-retrieval-response")
                                  .getJSONObject("coredata")
                                  .getJSONArray("link");

            for (int i = 0; i < links.length(); i++) {
                JSONObject link = links.getJSONObject(i);
                if (link.getString("@rel").equals("scidir")) {
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
