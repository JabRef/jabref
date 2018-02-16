package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.FieldName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for working with Eprint identifiers
 *
 * @see https://arxiv.org/help/arxiv_identifier
 * @see https://arxiv.org/hypertex/bibstyles/
 */
public class Eprint implements Identifier {
    public static final URI RESOLVER = URI.create("http://arxiv.org");
    private static final Logger LOGGER = LoggerFactory.getLogger(Eprint.class);

    // Regex
    // (see https://arxiv.org/help/arxiv_identifier)
    private static final String EPRINT_EXP = ""
            + "(?:arXiv:)?"                       // optional prefix
            + "("                               // begin group \1
            + "\\d{4}"                          // YYMM
            + "\\."                             // divider
            + "\\d{4,5}"                        // number
            + "(v\\d+)?"                        // optional version
            + "|"                               // old id
            + ".+"                              // archive
            + "(\\.\\w{2})?"                    // optional subject class
            + "\\/"                             // divider
            + "\\d{7}"                          // number
            + ")";                              // end group \1
    private static final String HTTP_EXP = "https?://[^\\s]+?" + EPRINT_EXP;
    // Pattern
    private static final Pattern EXACT_EPRINT_PATT = Pattern.compile("^(?:https?://[^\\s]+?)?" + EPRINT_EXP + "$", Pattern.CASE_INSENSITIVE);

    // DOI
    private final String eprint;

    /**
     * Creates a Eprint from various schemes including URL.
     *
     * @param eprint the Eprint identifier string
     * @throws NullPointerException if eprint is null
     * @throws IllegalArgumentException if eprint does not include a valid Eprint identifier
     * @return an instance of the Eprint class
     */
    public Eprint(String eprint) {
        Objects.requireNonNull(eprint);

        // Remove whitespace
        String trimmedId = eprint.trim();

        // HTTP URL decoding
        if (eprint.matches(HTTP_EXP)) {
            try {
                // decodes path segment
                URI url = new URI(trimmedId);
                trimmedId = url.getScheme() + "://" + url.getHost() + url.getPath();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(eprint + " is not a valid HTTP Eprint identifier.");
            }
        }

        // Extract DOI
        Matcher matcher = EXACT_EPRINT_PATT.matcher(trimmedId);
        if (matcher.find()) {
            // match only group \1
            this.eprint = matcher.group(1);
        } else {
            throw new IllegalArgumentException(trimmedId + " is not a valid Eprint identifier.");
        }
    }

    /**
     * Creates an Optional<Eprint> from various schemes including URL.
     *
     * Useful for suppressing the <c>IllegalArgumentException</c> of the Constructor
     * and checking for Optional.isPresent() instead.
     *
     * @param eprint the Eprint string
     * @return an Optional containing the Eprint or an empty Optional
     */
    public static Optional<Eprint> build(String eprint) {
        try {
            return Optional.ofNullable(new Eprint(eprint));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /**
     * Return a URI presentation for the Eprint identifier
     *
     * @return an encoded URI representation of the Eprint identifier
     */
    @Override
    public Optional<URI> getExternalURI() {
        try {
            URI uri = new URI(RESOLVER.getScheme(), RESOLVER.getHost(), "/abs/" + eprint, null);
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            // should never happen
            LOGGER.error(eprint + " could not be encoded as URI.", e);
            return Optional.empty();
        }
    }

    /**
     * Return an ASCII URL presentation for the Eprint identifier
     *
     * @return an encoded URL representation of the Eprint identifier
     */
    public String getURIAsASCIIString() {
        return getExternalURI().map(URI::toASCIIString).orElse("");
    }

    /**
     * Return the plain Eprint identifier
     *
     * @return the plain Eprint value.
     */
    public String getEprint() {
        return eprint;
    }

    @Override
    public String getDefaultField() {
        return FieldName.EPRINT;
    }

    @Override
    public String getNormalized() {
        return eprint;
    }
}
