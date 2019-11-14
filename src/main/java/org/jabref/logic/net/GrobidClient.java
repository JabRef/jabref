package org.jabref.logic.net;

/**
 * Implements an API to a GROBID server, as described at
 * https://grobid.readthedocs.io/en/latest/Grobid-service/#grobid-web-services
 *
 * The methods are structured to match the GROBID server api.
 * The interfaces using PDFs as parameters are not implemented yet, but feel free to add them if you want to
 * expand the functionality of JabRef.
 */
public class GrobidClient {

    public static String processDate(String rawDate) {
        HttpPostService.sendPost();
    }

}
