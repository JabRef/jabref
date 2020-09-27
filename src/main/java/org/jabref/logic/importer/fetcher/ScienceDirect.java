package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.net.URLDownload;
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
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at ScienceDirect. See <a href="https://dev.elsevier.com/">https://dev.elsevier.com/</a>
 */
public class ScienceDirect implements FulltextFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScienceDirect.class);

    private static final String API_URL = "http://api.elsevier.com/content/article/doi/";
    private static final String API_KEY = "fb82f2e692b3c72dafe5f4f1fa0ac00b";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        // Try unique DOI first
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

        if (doi.isPresent()) {
            // Available in catalog?
            try {
                String sciLink = getUrlByDoi(doi.get().getDOI());

                // scrape the web page not as mobile client!
                if (!sciLink.isEmpty()) {
                    Document html = Jsoup.connect(sciLink)
                                         .userAgent(URLDownload.USER_AGENT)
                                         .referrer("http://www.google.com")
                                         .ignoreHttpErrors(true).get();

                    // Retrieve PDF link from meta data (most recent)
                    Elements metaLinks = html.getElementsByAttributeValue("name", "citation_pdf_url");

                    if (!metaLinks.isEmpty()) {
                        String link = metaLinks.first().attr("content");
                        return Optional.of(new URL(link));
                    }

                    URL url = new URL(sciLink);
                    String protocol = url.getProtocol();
                    String authority = url.getAuthority();

                    Optional<String> fullLinkToPdf = html
                            .getElementsByAttributeValue("type", "application/json")
                            .stream()
                            .flatMap(element -> element.getElementsByTag("script").stream())
                            // get the text element
                            .map(element -> element.childNode(0))
                            .map(element -> element.toString())
                            .map(text -> new JSONObject(text))
                            .filter(json -> json.has("article"))
                            .map(json -> json.getJSONObject("article"))
                            .filter(json -> json.has("pdfDownload"))
                            .map(json -> json.getJSONObject("pdfDownload"))
                            .filter(json -> json.has("linkToPdf"))
                            .map(json -> json.getString("linkToPdf"))
                            .map(linkToPdf -> String.format("%s://%s%s", protocol, authority, linkToPdf))
                            .findAny();
                    if (fullLinkToPdf.isPresent()) {
                        LOGGER.info("Fulltext PDF found at ScienceDirect.");
                        // new URL may through "MalformedURLException", thus using "isPresent()" above and ".get()"
                        Optional<URL> pdfLink = Optional.of(new URL(fullLinkToPdf.get()));
                        return pdfLink;
                    }
                }
            } catch (UnirestException e) {
                LOGGER.warn("ScienceDirect API request failed", e);
            }
        }
        return Optional.empty();
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
            JSONArray links = json.getJSONObject("full-text-retrieval-response").getJSONObject("coredata").getJSONArray("link");

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
