package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for working with <a href="https://en.wikipedia.org/wiki/Digital_object_identifier">Digital object identifiers (DOIs)</a> and <a href="http://shortdoi.org">Short DOIs</a>
 */
public class DOI implements Identifier {

    public static final URI AGENCY_RESOLVER = URI.create("https://doi.org/doiRA");
    public static final URI RESOLVER = URI.create("https://doi.org/");

    private static final Logger LOGGER = LoggerFactory.getLogger(DOI.class);

    // Regex
    // (see http://www.doi.org/doi_handbook/2_Numbering.html)
    private static final String DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "(?:\\.[0-9]+)+"                  // registrant codes
            + "[/:%]" // divider
            + "(?:.+)"                          // suffix alphanumeric string
            + ")";                              // end group \1
    private static final String FIND_DOI_EXP = ""
            + "(?:urn:)?"                       // optional urn
            + "(?:doi:)?"                       // optional doi
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "(?:\\.[0-9]+)+"                  // registrant codes
            + "[/:]"                            // divider
            + "(?:[^\\s,;]+[^,;(\\.\\s)])"      // suffix alphanumeric without " "/","/";" and not ending on "."/","/";"
            + ")";                              // end group \1

    // Regex (Short DOI)
    private static final String SHORT_DOI_SHORTCUT = ""
            + "^\\s*(?:https?://)?(?:www\\.)?(?:doi\\.org/)([a-z0-9]{4,10})\\s*$"; // eg https://doi.org/bfrhmx
    private static final String IN_TEXT_SHORT_DOI_SHORTCUT = ""
            + "(?:https?://)?(?:www\\.)?(?:doi\\.org/)([a-z0-9]{4,10})"; // eg https://doi.org/bfrhmx somewhere in the text
    private static final String SHORT_DOI_EXP_PREFIX = ""
            + "^(?:" // can begin with...
            + "\\s*(?:https?://)?(?:www\\.)?"   // optional url parts "http(s)://"+"www."
            + "[a-zA-Z\\.]*doi[a-zA-Z\\.]*"     //  eg "dx.doi." or "doi.acm." or "doi." if with url, must include "doi", otherwise too ambiguous
            + "\\.[a-zA-Z]{2,10}/)?";           // ".org" or ".de" or ".academy"
    private static final String SHORT_DOI_EXP = ""
            + "(?:"                             // begin "any one of these"
            + "(?:[\\s/]?(?:(?:urn:)|(?:doi:)|(?:urn:doi:)))" // "doi:10/12ab" or " urn:10/12ab" or "/urn:doi:/10/12ab" ...
            + "|(?:\\s?/?)"                     // or "/10/12ab" or " /10/12ab" or "10/12ab" or " 10/12ab"
            + ")"                               // end "any one of these"
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "[/%:]"                           // divider
            + "[a-zA-Z0-9]{3,}"                 // at least 3 characters
            + ")"                               // end group  \1
            + "\\s*$";                          // must be the end
    private static final String FIND_SHORT_DOI_EXP = ""
            + "(?:"                             // begin "any one of these" (but not none of those!)
            + "(?:(?:www\\.)?doi\\.org/)"       // either doi.org
            + "|"                               // or any of the following with doi.org or not...
            + "(?:(?:doi.org/)?(?:(?:urn:)|(?:doi:)|(?:urn:doi:)))" // "doi:10/12ab" or " urn:10/12ab" or "/urn:doi:/10/12ab" or "doi.org/doi:10/12ab"...
            + ")"                               // end "any one of these"
            + "("                               // begin group \1
            + "10"                              // directory indicator
            + "[/%:]"                           // divider
            + "[a-zA-Z0-9]{3,}"                 // at least 3 characters
            + ")";                              // end group  \1

    private static final String HTTP_EXP = "https?://[^\\s]+?" + DOI_EXP;
    private static final String SHORT_DOI_HTTP_EXP = "https?://[^\\s]+?" + SHORT_DOI_EXP;
    // Pattern
    private static final Pattern EXACT_DOI_PATT = Pattern.compile("^(?:https?://[^\\s]+?)?" + DOI_EXP + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIND_DOI_PATT = Pattern.compile("(?:https?://[^\\s]+?)?" + FIND_DOI_EXP, Pattern.CASE_INSENSITIVE);
    // Pattern (short DOI)
    private static final Pattern EXACT_SHORT_DOI_SHORTCUT = Pattern.compile(SHORT_DOI_SHORTCUT, Pattern.CASE_INSENSITIVE); // eg doi.org/bfrhmx (no "10/")
    private static final Pattern FIND_SHORT_DOI_SHORTCUT = Pattern.compile(IN_TEXT_SHORT_DOI_SHORTCUT, Pattern.CASE_INSENSITIVE); // eg doi.org/bfrhmx (no "10/")
    private static final Pattern EXACT_SHORT_DOI_PATT = Pattern.compile(SHORT_DOI_EXP_PREFIX + SHORT_DOI_EXP, Pattern.CASE_INSENSITIVE);
    private static final Pattern FIND_SHORT_DOI_PATT = Pattern.compile("(?:https?://[^\\s]+?)?" + FIND_SHORT_DOI_EXP, Pattern.CASE_INSENSITIVE);

    // See https://www.baeldung.com/java-regex-s-splus for explanation of \\s+
    // See https://stackoverflow.com/questions/3203190/regex-any-ascii-character for the regexp that includes ASCII characters only
    // Another reference for regexp for ASCII characters: https://howtodoinjava.com/java/regex/java-clean-ascii-text-non-printable-chars/
    private static final String CHARS_TO_REMOVE = "[\\s+" // remove white space characters, i.e, \t, \n, \x0B, \f, \r . + is a greedy quantifier
                                                + "[^\\x00-\\x7F]" // strips off all non-ASCII characters
                                                + "]";

    // DOI
    private final String doi;
    // Short DOI
    private boolean isShortDoi = false;

    /**
     * Creates a DOI from various schemes including URL, URN, and plain DOIs/Short DOIs.
     *
     * @param doi the DOI/Short DOI string
     * @throws NullPointerException     if DOI/Short DOI is null
     * @throws IllegalArgumentException if doi does not include a valid DOI/Short DOI
     */
    public DOI(String doi) {
        Objects.requireNonNull(doi);

        // Remove whitespace
        String trimmedDoi = doi.trim();

        // HTTP URL decoding
        if (doi.matches(HTTP_EXP) || doi.matches(SHORT_DOI_HTTP_EXP)) {
            try {
                // decodes path segment
                URI url = new URI(trimmedDoi);
                trimmedDoi = url.getScheme() + "://" + url.getHost() + url.getPath();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(doi + " is not a valid HTTP DOI/Short DOI.");
            }
        }

        // Extract DOI/Short DOI
        Matcher matcher = EXACT_DOI_PATT.matcher(trimmedDoi);
        if (matcher.find()) {
            // match only group \1
            this.doi = matcher.group(1);
        } else {
            // Short DOI
            Matcher shortDoiMatcher = EXACT_SHORT_DOI_PATT.matcher(trimmedDoi);
            if (shortDoiMatcher.find()) {
                this.doi = shortDoiMatcher.group(1);
                isShortDoi = true;
            } else {
                // Shortcut DOI without the "10/" as in "doi.org/d8dn"
                Matcher shortcutDoiMatcher = EXACT_SHORT_DOI_SHORTCUT.matcher(trimmedDoi);
                if (shortcutDoiMatcher.find()) {
                    this.doi = "10/" + shortcutDoiMatcher.group(1);
                    isShortDoi = true;
                } else {
                    throw new IllegalArgumentException(trimmedDoi + " is not a valid DOI/Short DOI.");
                }
            }
        }
    }

    /**
     * Creates an Optional&lt;DOI> from various schemes including URL, URN, and plain DOIs.
     * <p>
     * Useful for suppressing the <c>IllegalArgumentException</c> of the Constructor and checking for Optional.isPresent() instead.
     *
     * @param doi the DOI/Short DOI string
     * @return an Optional containing the DOI or an empty Optional
     */
    public static Optional<DOI> parse(String doi) {
        try {
            String cleanedDOI = doi;
            cleanedDOI = cleanedDOI.replaceAll(CHARS_TO_REMOVE, "");

            return Optional.of(new DOI(cleanedDOI));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /**
     * Determines whether a DOI/Short DOI is valid or not
     *
     * @param doi the DOI/Short DOI string
     * @return true if DOI is valid, false otherwise
     */
    public static boolean isValid(String doi) {
        return parse(doi).isPresent();
    }

    /**
     * Tries to find a DOI/Short DOI inside the given text.
     *
     * @param text the Text which might contain a DOI/Short DOI
     * @return an Optional containing the DOI or an empty Optional
     */
    public static Optional<DOI> findInText(String text) {
        Optional<DOI> result = Optional.empty();

        Matcher matcher = FIND_DOI_PATT.matcher(text);
        if (matcher.find()) {
            // match only group \1
            result = Optional.of(new DOI(matcher.group(1)));
        }

        matcher = FIND_SHORT_DOI_PATT.matcher(text);
        if (matcher.find()) {
            result = Optional.of(new DOI(matcher.group(1)));
        }

        matcher = FIND_SHORT_DOI_SHORTCUT.matcher(text);
        if (matcher.find()) {
            result = Optional.of(new DOI(matcher.group(0)));
        }

        return result;
    }

    @Override
    public String toString() {
        return "DOI{" +
                "doi='" + doi + '\'' +
                '}';
    }

    /**
     * Return the plain DOI/Short DOI
     *
     * @return the plain DOI/Short DOI value.
     */
    public String getDOI() {
        return doi;
    }

    /**
     * Determines whether DOI is short DOI or not
     *
     * @return true if DOI is short DOI, false otherwise
     */
    public boolean isShortDoi() {
        return isShortDoi;
    }

    /**
     * Return a URI presentation for the DOI/Short DOI
     *
     * @return an encoded URI representation of the DOI/Short DOI
     */
    @Override
    public Optional<URI> getExternalURI() {
        // TODO: We need dependency injection here. It should never happen that this method is called.
        //       Always, the user preferences should be honored --> #getExternalURIWithCustomBase
        return getExternalURIFromBase(RESOLVER);
    }

    public Optional<URI> getExternalURIWithCustomBase(String customBase) {
        return getExternalURIFromBase(URI.create(customBase));
    }

    private Optional<URI> getExternalURIFromBase(URI base) {
        try {
            URI uri = new URI(base.getScheme(), base.getHost(), "/" + doi, null);
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            // should never happen
            LOGGER.error(doi + " could not be encoded as URI.", e);
            return Optional.empty();
        }
    }

    /**
     * Return an ASCII URL presentation for the DOI/Short DOI
     *
     * @return an encoded URL representation of the DOI/Short DOI
     */
    public String getURIAsASCIIString() {
        return getExternalURI().map(URI::toASCIIString).orElse("");
    }

    @Override
    public Field getDefaultField() {
        return StandardField.DOI;
    }

    @Override
    public String getNormalized() {
        return doi;
    }

    /**
     * DOIs are case-insensitive. Thus, 10.1109/cloud.2017.89 equals 10.1109/CLOUD.2017.89
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        DOI other = (DOI) o;
        return doi.equalsIgnoreCase(other.doi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doi.toLowerCase(Locale.ENGLISH));
    }
}
