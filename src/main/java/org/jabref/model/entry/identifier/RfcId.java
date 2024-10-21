package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an RFC identifier, which can be used to fetch bibliographic information about the RFC document.
 * This class supports both plain RFC IDs (e.g., "rfc7276") and full URLs (e.g., "https://www.rfc-editor.org/rfc/" + rfc****.html).
 */
public class RfcId extends EprintIdentifier {
    private static final String RFC_URL_REGEX = "(https?://)?(www\\.)?rfc-editor\\.org/rfc/rfc(?<id>\\d+)(\\.html)?.*";
    private static final Pattern RFC_URL_MATCH = Pattern.compile(RFC_URL_REGEX, Pattern.CASE_INSENSITIVE);
    private final String rfcString;

    /**
     * Constructs an RfcId object from a given string.
     *
     * @param rfcString The RFC ID or URL, which will be converted to lowercase and trimmed.
     * @throws NullPointerException if the given rfcString is null.
     */
    public RfcId(String rfcString) {
        this.rfcString = Objects.requireNonNull(rfcString).trim().toLowerCase();
    }

    /**
     * Parses the given input string to extract a valid RFC identifier.
     * The input can be a plain RFC ID or a full URL.
     *
     * @param input The input string, which could be in the form of an RFC ID (e.g., "rfc1234") or a complete URL.
     * @return An Optional containing a valid RfcId if the input matches the expected pattern, otherwise an empty Optional.
     */
    public static Optional<RfcId> parse(String input) {
        Matcher urlMatcher = RFC_URL_MATCH.matcher(input);
        if (urlMatcher.matches()) {
            String rfcId = "rfc" + urlMatcher.group("id");
            return Optional.of(new RfcId(rfcId));
        }
        return Optional.empty();
    }

    /**
     * Checks if the stored RFC string is valid according to the defined RFC pattern.
     *
     * @return true if the RFC string matches the expected pattern, otherwise false.
     */
    public boolean isValid() {
        return RFC_URL_MATCH.matcher(rfcString).matches();
    }

    @Override
    public String getNormalized() {
        return rfcString;
    }

    /**
     * Generates an external URI that points to the RFC document on the rfc-editor website.
     *
     * @return An Optional containing the URI if the construction succeeds, otherwise an empty Optional.
     */
    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://www.rfc-editor.org/rfc/" + rfcString));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
