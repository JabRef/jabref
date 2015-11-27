package net.sf.jabref.logic.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for working with Digital object identifiers (DOIs)
 *
 * @see https://en.wikipedia.org/wiki/Digital_object_identifier
 */
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

    /**
     * Creates an Optional<DOI> from various schemes including URL, URN, and plain DOIs.
     *
     * Useful for suppressing the <c>IllegalArgumentException</c> of the Constructor
     * and checking for Optional.isPresent() instead.
     *
     * @param doi the DOI string
     * @return an Optional containing the DOI or an empty Optional
     */
    public static Optional<DOI> build(String doi) {
        try {
            return Optional.ofNullable(new DOI(doi));
        } catch(IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // DOI
    private final String doi;

    /**
     * Creates a DOI from various schemes including URL, URN, and plain DOIs.
     *
     * @param doi the DOI string
     * @throws NullPointerException if DOI is null
     * @throws IllegalArgumentException if doi does not include a valid DOI
     * @return an instance of the DOI class
     */
    public DOI(String doi) {
        Objects.requireNonNull(doi);

        // Remove whitespace
        doi = doi.trim();

        // HTTP URL decoding
        if(doi.matches(HTTP_EXP)) {
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
     * Return the plain DOI
     *
     * @return the plain DOI value.
     */
    public String getDOI() {
        return doi;
    }

    /**
     * Return a URI presentation for the DOI
     *
     * @return an encoded URI representation of the DOI
     */
    public URI getURI() {
        try {
            URI uri = new URI(RESOLVER.getScheme(), RESOLVER.getHost(), "/" + doi, null);
            return uri;
        } catch(URISyntaxException e) {
            // should never happen
            LOGGER.error(doi + " could not be encoded as URI.", e);
            return null;
        }
    }

    /**
     * Return an ASCII URL presentation for the DOI
     *
     * @return an encoded URL representation of the DOI
     */
    public String getURLAsASCIIString() {
        return getURI().toASCIIString();
    }
}
