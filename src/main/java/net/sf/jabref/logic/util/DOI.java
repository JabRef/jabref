package net.sf.jabref.logic.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for working with Digital object identifiers (DOIs)
 *
 * @see https://en.wikipedia.org/wiki/Digital_object_identifier
 */
public class DOI {
    private static final Log LOGGER = LogFactory.getLog(DOI.class);

    // DOI resolver
    public static final URI RESOLVER = URI.create("http://doi.org");

    // DOI
    private final String doi;

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

    private static final String FIND_DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "(?:\\.[0-9]+)+"                  // registrant codes
            + "[/:]"                            // divider
            + "(?:[^\\s]+)"                     // suffix alphanumeric without space
            + ")";                              // end group \1

    private static final String HTTP_EXP = "https?://[^\\s]+?" + DOI_EXP;
    // Pattern
    private static final Pattern EXACT_DOI_PATT = Pattern.compile("^(?:https?://[^\\s]+?)?" + DOI_EXP + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOI_PATT = Pattern.compile("(?:https?://[^\\s]+?)?" + FIND_DOI_EXP, Pattern.CASE_INSENSITIVE);

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
        String trimmedDoi = doi.trim();

        // HTTP URL decoding
        if(doi.matches(HTTP_EXP)) {
            try {
                // decodes path segment
                URI url = new URI(trimmedDoi);
                trimmedDoi = url.getScheme() + "://" + url.getHost() + url.getPath();
            } catch(URISyntaxException e) {
                throw new IllegalArgumentException(doi + " is not a valid HTTP DOI.");
            }
        }

        // Extract DOI
        Matcher matcher = EXACT_DOI_PATT.matcher(trimmedDoi);
        if (matcher.find()) {
            // match only group \1
            this.doi = matcher.group(1);
        } else {
            throw new IllegalArgumentException(trimmedDoi + " is not a valid DOI.");
        }
    }

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
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /**
     * Tries to find a DOI inside the given text.
     *
     * @param text the Text which might contain a DOI
     * @return an Optional containing the DOI or an empty Optional
     */
    public static Optional<DOI> findInText(String text) {
        Optional<DOI> result = Optional.empty();

        Matcher matcher = DOI_PATT.matcher(text);
        if (matcher.find()) {
            // match only group \1
            result = Optional.of(new DOI(matcher.group(1)));
        }
        return result;
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
    public Optional<URI> getURI() {
        try {
            URI uri = new URI(RESOLVER.getScheme(), RESOLVER.getHost(), "/" + doi, null);
            return Optional.of(uri);
        } catch(URISyntaxException e) {
            // should never happen
            LOGGER.error(doi + " could not be encoded as URI.", e);
            return Optional.empty();
        }
    }

    /**
     * Return an ASCII URL presentation for the DOI
     *
     * @return an encoded URL representation of the DOI
     */
    public String getURLAsASCIIString() {
        return getURI().map(URI::toASCIIString).orElse("");
    }
}
