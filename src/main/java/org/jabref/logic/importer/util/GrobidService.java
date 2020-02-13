package org.jabref.logic.importer.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.net.URLDownload;

/**
 * Implements an API to a GROBID server, as described at
 * https://grobid.readthedocs.io/en/latest/Grobid-service/#grobid-web-services
 * <p>
 * Note: Currently a custom GROBID server is used...
 * https://github.com/NikodemKch/grobid
 * <p>
 * The methods are structured to match the GROBID server api.
 * Each method corresponds to a GROBID service request. Only the ones already used are already implemented.
 */
public class GrobidService {

    public enum ConsolidateCitations {
        NO(0), WITH_METADATA(1), WITH_DOI_ONLY(2);
        private int code;

        ConsolidateCitations(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }
    }

    String grobidServerURL;

    public GrobidService(String grobidServerURL) {
        this.grobidServerURL = grobidServerURL;
    }

    public String processCitation(String rawCitation, ConsolidateCitations consolidateCitations) throws IOException {
        rawCitation = URLEncoder.encode(rawCitation, StandardCharsets.UTF_8);
        URLDownload urlDownload = new URLDownload(grobidServerURL
                + "/api/processCitation");
        urlDownload.setPostData("citations=" + rawCitation + "&consolidateCitations=" + consolidateCitations);
        String httpResponse = urlDownload.asString();

        if (httpResponse == null || httpResponse.equals("@misc{-1,\n\n}\n")) { //This filters empty BibTeX entries
            throw new IOException("The GROBID server response does not contain anything.");
        }

        return httpResponse;
    }
}
