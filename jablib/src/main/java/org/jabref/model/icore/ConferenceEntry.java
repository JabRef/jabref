package org.jabref.model.icore;

/**
 * A Conference Entry built from a subset of fields in the ICORE Ranking data
 */
public record ConferenceEntry(
        String id,
        String title,
        String acronym,
        String rank
) {
    private final static String URL_PREFIX = "https://portal.core.edu.au/conf-ranks/";

    public String getICOREURL() {
        return URL_PREFIX + id;
    }
}
