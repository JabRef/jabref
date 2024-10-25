package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an SSRN article, identified by its abstract ID.
 */
public class SSRN extends EprintIdentifier {

    private static final String SSRN_URL_REGEX = "(https?://)?(papers\\.)?ssrn\\.com/(sol3/papers.cfm\\?)?abstract(_id)?=(?<id>\\d+)";
    private static final Pattern SSRN_URL_FULL_MATCH = Pattern.compile("^" + SSRN_URL_REGEX + "$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SSRN_URL_MATCH = Pattern.compile(SSRN_URL_REGEX, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    private final Integer abstractId;

    /**
     * Tries to parse an SSRN identifier
     *
     * @param string Either a number or a SSRN url that has the abstract ID in it
     * @throws NullPointerException If you pass a null to the constructor
     * @throws IllegalArgumentException Invalid string passed to the constructor
     */
    public SSRN(String string) {
        Objects.requireNonNull(string);
        string = string.trim();

        if (string.chars().allMatch(Character::isDigit)) {
            abstractId = Integer.parseInt(string);
            return;
        }

        Matcher matcher = SSRN_URL_FULL_MATCH.matcher(string);
        if (matcher.find()) {
            abstractId = Integer.parseInt(matcher.group("id"));
            return;
        }

        throw new IllegalArgumentException(string + " is not a valid SSRN identifier");
    }

    public SSRN(Integer abstractId) {
        this.abstractId = abstractId;
    }

    public static Optional<SSRN> parse(String data) {
        Matcher matcher = SSRN_URL_MATCH.matcher(data);
        if (matcher.find()) {
            int abstractId = Integer.parseInt(matcher.group("id"));
            return Optional.of(new SSRN(abstractId));
        }

        return Optional.empty();
    }

    @Override
    public String asString() {
        return String.valueOf(abstractId);
    }

    @Override
    public Optional<URI> getExternalURI() {
        URI uri = URI.create("https://ssrn.com/abstract=" + abstractId);
        return Optional.of(uri);
    }

    /**
     * Generate the DOI based on the SSRN
     *
     * @return a DOI formatted as <code>10.2139/ssrn.[article]</code>
     */
    public DOI toDoi() {
        return new DOI("10.2139/ssrn." + abstractId);
    }
}
