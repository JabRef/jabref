package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Class for working with <a href="https://en.wikipedia.org/wiki/Digital_object_identifier">Digital object identifiers (DOIs)</a> and <a href="http://shortdoi.org">Short DOIs</a>
@AllowedToUseLogic("because we want to have this class 'smart' an be able to parse obscure DOIs, too. For this, we need the LatexToUnicodeformatter.")
@NullMarked
public class DOI implements Identifier {

    public static final URI AGENCY_RESOLVER = URLUtil.createUri("https://doi.org/doiRA");
    public static final URI RESOLVER = URLUtil.createUri("https://doi.org/");

    private static final Logger LOGGER = LoggerFactory.getLogger(DOI.class);

    private static final String DOI_GROUP = "doi";

    // Regex
    // (see http://www.doi.org/doi_handbook/2_Numbering.html)
    private static final String DOI_EXP = "(?:urn:)?"       // optional urn
            + "(?:doi:)?"                                   // optional doi
            + "(?<" + DOI_GROUP + ">"                       // begin named group
            + "10"                                          // directory indicator
            + "(?:\\.[0-9]+)+"                              // registrant codes
            + "[/:%]"                                       // divider
            + "(?:.+)"                                      // suffix alphanumeric string
            + ")";                                          // end named group
    private static final String FIND_DOI_EXP = "(?:urn:)?"  // optional urn
            + "(?:doi:)?"                                   // optional doi
            + "(?<" + DOI_GROUP + ">"                       // begin named group
            + "10"                                          // directory indicator
            + "(?:\\.[0-9]+)+"                              // registrant codes
            + "[/:]"                                        // divider
            + "(?:[^\\s,]+[^,;(\\.\\s)])"                   // suffix alphanumeric without " "/"," and not ending on "."/","/";"
            + ")";                                          // end named group

    // Regex (Short DOI)
    private static final String SHORT_DOI_SHORTCUT = "^\\s*(?:https?://)?(?:www\\.)?(?:doi\\.org/)(?<" + DOI_GROUP + ">[a-z0-9]{4,10})\\s*$"; // eg https://doi.org/bfrhmx
    private static final String IN_TEXT_SHORT_DOI_SHORTCUT = "(?:https?://)?(?:www\\.)?(?:doi\\.org/)(?<" + DOI_GROUP + ">[a-z0-9]{4,10})"; // eg https://doi.org/bfrhmx somewhere in the text
    private static final String SHORT_DOI_EXP_PREFIX = "^(?:"   // can begin with...
            + "\\s*(?:https?://)?(?:www\\.)?"                   // optional url parts "http(s)://"+"www."
            + "[a-zA-Z\\.]*doi[a-zA-Z\\.]*"                     //  eg "dx.doi." or "doi.acm." or "doi." if with url, must include "doi", otherwise too ambiguous
            + "\\.[a-zA-Z]{2,10}/)?";                           // ".org" or ".de" or ".academy"
    private static final String SHORT_DOI_EXP = "(?:"           // begin "any one of these"
            + "(?:[\\s/]?(?:(?:urn:)|(?:doi:)|(?:urn:doi:)))"   // "doi:10/12ab" or " urn:10/12ab" or "/urn:doi:/10/12ab" ...
            + "|(?:\\s?/?)"                                     // or "/10/12ab" or " /10/12ab" or "10/12ab" or " 10/12ab"
            + ")"                                               // end "any one of these"
            + "(?<" + DOI_GROUP + ">"                           // begin named group
            + "10"                                              // directory indicator
            + "[/%:]"                                           // divider
            + "[a-zA-Z0-9]{3,}"                                 // at least 3 characters
            + ")"                                               // end named group
            + "\\s*$";                                          // must be the end
    private static final String FIND_SHORT_DOI_EXP = "(?:"          // begin "any one of these" (but not none of those!)
            + "(?:(?:www\\.)?doi\\.org/)"                           // either doi.org
            + "|"                                                   // or any of the following with doi.org or not...
            + "(?:(?:doi.org/)?(?:(?:urn:)|(?:doi:)|(?:urn:doi:)))" // "doi:10/12ab" or " urn:10/12ab" or "/urn:doi:/10/12ab" or "doi.org/doi:10/12ab"...
            + ")"                                                   // end "any one of these"
            + "(?<" + DOI_GROUP + ">"                               // begin named group
            + "10"                                                  // directory indicator
            + "[/%:]"                                               // divider
            + "[a-zA-Z0-9]{3,}"                                     // at least 3 characters
            + ")";                                                  // end named group

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
            + "\\\\"            // remove backslashes
            + "{}"              // remove curly brackets
            + "\\[\\]`|"        // remove square brackets, backticks, and pipes
            + "[^\\x00-\\x7F]"  // strips off all non-ASCII characters
            + "]";

    // DOI
    private final String doi;
    // Short DOI
    private boolean isShortDoi = false;

    /// Creates a DOI from various schemes including URL, URN, and plain DOIs/Short DOIs.
    ///
    /// @param doi the DOI/Short DOI string
    /// @throws NullPointerException     if DOI/Short DOI is null
    /// @throws IllegalArgumentException if doi does not include a valid DOI/Short DOI
    public DOI(String doi) {
        // Remove whitespace
        String trimmedDoi = doi.trim();

        // HTTP URL decoding
        if (doi.matches(HTTP_EXP) || doi.matches(SHORT_DOI_HTTP_EXP)) {
            // decodes path segment
            trimmedDoi = URLDecoder.decode(trimmedDoi, StandardCharsets.UTF_8);
        }

        // Extract DOI/Short DOI
        Matcher matcher = EXACT_DOI_PATT.matcher(trimmedDoi);
        if (matcher.find()) {
            this.doi = matcher.group(DOI_GROUP);
        } else {
            // Short DOI
            Matcher shortDoiMatcher = EXACT_SHORT_DOI_PATT.matcher(trimmedDoi);
            if (shortDoiMatcher.find()) {
                this.doi = shortDoiMatcher.group(DOI_GROUP);
                isShortDoi = true;
            } else {
                // Shortcut DOI without the "10/" as in "doi.org/d8dn"
                Matcher shortcutDoiMatcher = EXACT_SHORT_DOI_SHORTCUT.matcher(trimmedDoi);
                if (shortcutDoiMatcher.find()) {
                    this.doi = "10/" + shortcutDoiMatcher.group(DOI_GROUP);
                    isShortDoi = true;
                } else {
                    throw new IllegalArgumentException(trimmedDoi + " is not a valid DOI/Short DOI.");
                }
            }
        }
    }

    /// Creates an `Optional<DOI>` from various schemes including URL, URN, and plain DOIs.
    ///
    /// Useful for suppressing the {@link java.lang.IllegalArgumentException IllegalArgumentException}
    /// of the constructor and checking for {@link java.util.Optional#isPresent} instead.
    ///
    /// @param doi the DOI/Short DOI string
    /// @return an Optional containing the DOI or an empty Optional
    public static Optional<DOI> parse(String doi) {
        try {
            LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();
            String cleanedDOI = doi;
            cleanedDOI = URLDecoder.decode(cleanedDOI, StandardCharsets.UTF_8);
            // needs to be handled before LatexToUnicode, because otherwise `^` will be treated as conversion superscript
            cleanedDOI = cleanedDOI.replaceAll("\\^", "");
            cleanedDOI = formatter.format(cleanedDOI);
            cleanedDOI = cleanedDOI.replaceAll(CHARS_TO_REMOVE, "");

            if (cleanedDOI.startsWith("_") && cleanedDOI.endsWith("_")) {
                if (cleanedDOI.length() == 1) {
                    return Optional.empty();
                }
                cleanedDOI = cleanedDOI.substring(1, cleanedDOI.length() - 1);
            }

            return Optional.of(new DOI(cleanedDOI));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    /// Determines whether a DOI/Short DOI is valid or not
    ///
    /// @param doi the DOI/Short DOI string
    /// @return true if DOI is valid, false otherwise
    public static boolean isValid(String doi) {
        return parse(doi).isPresent();
    }

    /// Tries to find a DOI/Short DOI inside the given text.
    ///
    /// @param text the text which might contain a DOI/Short DOI
    /// @return an Optional containing the DOI or an empty Optional
    public static Optional<DOI> findInText(String text) {
        return findInTextInternal(removeReplacementCharacters(text)).map(match -> new DOI(match.doi()));
    }

    /// Replaces the first DOI/Short DOI found in the text with the given
    /// replacement. The replaced region includes any URL or `doi:` prefix that
    /// was part of the match. Returns the text unchanged if no DOI is present.
    ///
    /// This reuses the same matching as {@link #findInText(String)} so callers
    /// that need to strip a DOI from text do not have to re-derive the regex.
    ///
    /// @param text        the text which might contain a DOI/Short DOI
    /// @param replacement the string to put in place of the matched DOI
    /// @return the text with the first DOI occurrence replaced
    public static String replaceInText(String text, String replacement) {
        String cleaned = removeReplacementCharacters(text);
        return findInTextInternal(cleaned)
                .map(match -> cleaned.substring(0, match.start()) + replacement + cleaned.substring(match.end()))
                .orElse(text);
    }

    /// Result of locating a DOI inside a text: the parsed DOI string plus the
    /// matched region (which may include a URL or `doi:` prefix).
    private record DoiTextMatch(String doi, int start, int end) {
    }

    private static String removeReplacementCharacters(String text) {
        return text.replaceAll("[�]", "");
    }

    /// @param text text that has already been passed through {@link #removeReplacementCharacters}
    private static Optional<DoiTextMatch> findInTextInternal(String text) {
        // Each pattern is tried independently; the earliest match in the text wins so
        // callers like {@link #replaceInText} strip the first DOI rather than whichever
        // regex happened to be tried last. The shortcut form (e.g. "doi.org/bfrhmx") is
        // re-prefixed with "10/" so the {@link DOI} constructor accepts it directly via
        // EXACT_SHORT_DOI_PATT instead of having to re-derive the URL form.
        return Stream.of(
                             firstMatch(FIND_DOI_PATT, text, m -> m.group(DOI_GROUP)),
                             firstMatch(FIND_SHORT_DOI_PATT, text, m -> m.group(DOI_GROUP)),
                             firstMatch(FIND_SHORT_DOI_SHORTCUT, text, m -> "10/" + m.group(DOI_GROUP)))
                     .flatMap(Optional::stream)
                     .min(Comparator.comparingInt(DoiTextMatch::start));
    }

    private static Optional<DoiTextMatch> firstMatch(Pattern pattern, String text, Function<Matcher, String> doiExtractor) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return Optional.of(new DoiTextMatch(doiExtractor.apply(matcher), matcher.start(), matcher.end()));
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "DOI{" +
                "doi='" + doi + '\'' +
                '}';
    }

    /// Return the plain DOI/Short DOI
    ///
    /// @return the plain DOI/Short DOI value.
    @Override
    public String asString() {
        return doi;
    }

    /// Determines whether DOI is short DOI or not
    ///
    /// @return true if DOI is short DOI, false otherwise
    public boolean isShortDoi() {
        return isShortDoi;
    }

    /// Return a URI presentation for the DOI/Short DOI
    ///
    /// TODO: Bad design, because Optional is used for Exception handling. Should be rewritten to throw a malformed URL exception.
    ///
    /// @return an encoded URI representation of the DOI/Short DOI
    @Override
    public Optional<URI> getExternalURI() {
        // TODO: We need dependency injection here. It should never happen that this method is called.
        //       Always, the user preferences should be honored --> #getExternalURIWithCustomBase.
        //       However, OpenAlex fetcher relies on this.
        return getExternalURIFromBase(RESOLVER);
    }

    public Optional<URI> getExternalURIWithCustomBase(String customBase) {
        return getExternalURIFromBase(URLUtil.createUri(customBase));
    }

    public Optional<URI> getExternalURIFromBase(URI base) {
        try {
            URI uri = new URI(base.getScheme(), base.getHost(), "/" + doi, null);
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            // should never happen
            LOGGER.error("{} could not be encoded as URI.", doi, e);
            return Optional.empty();
        }
    }

    /// Return an ASCII URL presentation for the DOI/Short DOI
    ///
    /// @return an encoded URL representation of the DOI/Short DOI. Empty string in the case of an error.
    public String getURIAsASCIIString() {
        return getExternalURI().map(URI::toASCIIString).orElse("");
    }

    @Override
    public Field getDefaultField() {
        return StandardField.DOI;
    }

    /// DOIs are case-insensitive. Thus, 10.1109/cloud.2017.89 equals 10.1109/CLOUD.2017.89
    @Override
    public boolean equals(@Nullable Object o) {
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
