package org.jabref.logic.importer.util;

import java.io.IOException;

import org.jabref.logic.net.URLDownload;
import org.jabref.preferences.JabRefPreferences;

/**
 * Implements an API to a GROBID server, as described at
 * https://grobid.readthedocs.io/en/latest/Grobid-service/#grobid-web-services
 *
 * Note: Currently a custom GROBID server is used...
 * https://github.com/NikodemKch/grobid
 *
 * The methods are structured to match the GROBID server api.
 * Each method corresponds to a GROBID service request. Only the ones already used are already implemented.
 */
public class GrobidService {

    private static JabRefPreferences jabRefPreferences;

    public GrobidService(JabRefPreferences jabRefPreferences) {
        GrobidService.jabRefPreferences = jabRefPreferences;
    }

    public String processCitation(String rawCitation, int consolidateCitations) throws GrobidServiceException {
      rawCitation = rawCitation.replace("&", "and");
      if (consolidateCitations < 0 || consolidateCitations > 2) {
            throw new GrobidServiceException("");
        }

        try {
            URLDownload urlDownload = new URLDownload(jabRefPreferences.get(JabRefPreferences.CUSTOM_GROBID_SERVER)
                    + "/api/processCitation");
            urlDownload.setPostData("citations=" + rawCitation + "&consolidateCitations=" + consolidateCitations);
            String httpResponse = urlDownload.asString();

            if (httpResponse == null) {
                throw new GrobidServiceException("The GROBID server response does not contain anything.");
            }

            return httpResponse;
        } catch (IOException e) {
            throw new GrobidServiceException("An I/O exception occurred while processing your request", e);
        }
    }
}
