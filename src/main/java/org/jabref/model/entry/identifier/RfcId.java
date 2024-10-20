package org.jabref.model.entry.identifier;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RfcId implements Identifier {

    private static final Pattern RFC_PATTERN = Pattern.compile("^rfc\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern RFC_URL_PATTERN = Pattern.compile("rfc(\\d+)", Pattern.CASE_INSENSITIVE);

    private final String rfcString;

    public RfcId(String rfcString) {
        this.rfcString = Objects.requireNonNull(rfcString).trim().toLowerCase();
    }

    public static Optional<RfcId> parse(String input) {
        Matcher rfcMatcher = RFC_PATTERN.matcher(input);
        if (rfcMatcher.matches()) {
            return Optional.of(new RfcId(input));
        }

        Matcher urlMatcher = RFC_URL_PATTERN.matcher(input);
        if (urlMatcher.find()) {
            String parsedRfcId = "rfc" + urlMatcher.group(1);
            return Optional.of(new RfcId(parsedRfcId));
        }

        return Optional.empty();
    }

    public boolean isValid() {
        Matcher rfcMatcher = RFC_PATTERN.matcher(rfcString);
        return rfcMatcher.matches();
    }

    @Override
    public String getNormalized() {
        return rfcString;
    }

    @Override
    public Field getDefaultField() {
        return StandardField.RFC;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://datatracker.ietf.org/doc/" + rfcString));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        RfcId other = (RfcId) o;
        return rfcString.equalsIgnoreCase(other.rfcString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rfcString.toLowerCase());
    }
}
