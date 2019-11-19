package org.jabref.logic.importer.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Implements an API to a GROBID server, as described at
 * https://grobid.readthedocs.io/en/latest/Grobid-service/#grobid-web-services
 *
 * The methods are structured to match the GROBID server api.
 * Each method corresponds to a GROBID service request. Only the ones already used are already implemented.
 */
public class GrobidService {

    public static String processCitation(String rawCitation, int consolidateCitations) throws GrobidServiceException {
        try {
            if (consolidateCitations < 0 || consolidateCitations > 2) {
                throw new GrobidServiceException("");
            }
            HttpEntity httpResponse = HttpPostService.sendPostAndWait(
                    Globals.prefs.get(JabRefPreferences.CUSTOM_GROBID_SERVER),
                    "/api/processCitation",
                    Map.of("citations", rawCitation, "consolidateCitations", String.valueOf(consolidateCitations))
                    ).getEntity();
            if (httpResponse == null) {
                throw new GrobidServiceException("The GROBID server response does not contain anything.");
            }
            InputStream serverResponseAsStream = httpResponse.getContent();
            return IOUtils.toString(serverResponseAsStream, StandardCharsets.UTF_8);
        } catch (HttpPostServiceException | IOException e) {
            throw new GrobidServiceException();
        }
    }

}
