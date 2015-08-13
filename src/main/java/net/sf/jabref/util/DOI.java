package net.sf.jabref.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOI {
    private static final Log LOGGER = LogFactory.getLog(DOI.class);

    // DOI resolver
    public static final URI RESOLVER = URI.create("http://doi.org");

    // Regex
    // (see http://www.doi.org/doi_handbook/2_Numbering.html)
    private static final String DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "(?:\\.[0-9]+)+"                  // registrant codes
            + "[/:]"                            // divider
            + "(?:.+)"                          // suffix alphanumeric string
            + ")";                              // end group \1

    private static final String HTTP_EXP = "https?://[^\\s]+?" + DOI_EXP;
    // Pattern
    private static final Pattern DOI_PATT = Pattern.compile("^(?:https?://[^\\s]+?)?" + DOI_EXP + "$", Pattern.CASE_INSENSITIVE);

    // DOI
    private String doi;

    /**
     * Creates a DOI from various schemes including URL, URN, and plain DOIs.
     *
     * @param doi the DOI string
     * @throws NullPointerException if DOI is null
     * @throws IllegalArgumentException if doi does not include a valid DOI
     */
    public DOI(String doi) {
        Objects.requireNonNull(doi);

        // Remove whitespace
        doi = doi.trim();

        // URL decoding
        if(isHttpDOI(doi)) {
            try {
                // decodes path segment
                URI url = new URI(doi);
                doi = url.getScheme() + "://" + url.getHost() + url.getPath();
            } catch(URISyntaxException e) {
                throw new IllegalArgumentException(doi + " is not a valid HTTP DOI.");
            }
        }

        // Extract DOI
        Matcher matcher = DOI_PATT.matcher(doi);
        if (matcher.find()) {
            // match only group \1
            this.doi = matcher.group(1);
        } else {
            throw new IllegalArgumentException(doi + " is not a valid DOI.");
        }
    }

    /**
     * Check if the String matches a plain DOI
     *
     * @param value the String to check
     * @return true if value contains a DOI
     */
    public static boolean isDOI(String value) {
        if(value == null)  {
            return false;
        }
        // whitespace
        value = value.trim();
        return value.matches(DOI_EXP);
    }

    /**
     * Check if the String matches a URI presentation of a DOI
     *
     * <example>
     *     The Doi name "10.1006/jmbi.1998.2354" would be made an actionable link as "http://doi.org/10.1006/jmbi.1998.2354".
     * </example>
     *
     * @param value the String to check
     * @return true if value contains a URI presentation of a DOI
     */
    public static boolean isHttpDOI(String value) {
        if(value == null)  {
            return false;
        }
        // whitespace
        value = value.trim();
        return value.matches(HTTP_EXP);
    }

    /**
     * Return the plain DOI
     *
     * @return the plain DOI value.
     */
    public String getDOI() {
        return doi;
    }

    /**
     * Return a URL presentation for the DOI
     *
     * @return an encoded URL representation of the DOI
     */
    public String getURL() {
        try {
            URI uri = new URI(RESOLVER.getScheme(), RESOLVER.getHost(), "/" + doi, null);
            return uri.toASCIIString();
        } catch(URISyntaxException e) {
            // should never happen
            LOGGER.error(doi + " could not be encoded as URL.");
            return "";
        }
    }
}
