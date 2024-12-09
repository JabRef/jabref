package org.jabref.logic.importer.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;

/**
 * Class for obtaining shortened DOI names. See <a href="https://shortdoi.org">https://shortdoi.org</a>.
 */
public class ShortDOIService {

    private static final String BASIC_URL = "http://shortdoi.org/";

    /**
     * Obtains shortened DOI name for given DOI
     *
     * @param doi DOI
     * @return A shortened DOI name
     */
    public DOI getShortDOI(DOI doi) throws ShortDOIServiceException {
        JSONObject responseJSON = makeRequest(doi);
        String shortDoi = responseJSON.getString("ShortDOI");

        return new DOI(shortDoi);
    }

    private JSONObject makeRequest(DOI doi) throws ShortDOIServiceException {

        URIBuilder uriBuilder;
        URL url;

        try {
            uriBuilder = new URIBuilder(BASIC_URL);
            uriBuilder.setPath(uriBuilder.getPath() + doi.asString());
            uriBuilder.addParameter("format", "json");

            URI uri = uriBuilder.build();
            url = uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new ShortDOIServiceException("Cannot get short DOI", e);
        }

        URLDownload urlDownload = new URLDownload(url);

        try {
            JSONObject resultAsJSON = JsonReader.toJsonObject(urlDownload.asInputStream());
            if (resultAsJSON.isEmpty()) {
                throw new ShortDOIServiceException("Cannot get short DOI");
            }
            return resultAsJSON;
        } catch (ParseException | JSONException | FetcherException e) {
            throw new ShortDOIServiceException("Cannot get short DOI", e);
        }
    }
}
