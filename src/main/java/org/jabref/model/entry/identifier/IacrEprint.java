package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IacrEprint implements Identifier {
    public static final URI RESOLVER = URI.create("https://ia.cr");
    private static final Logger LOGGER = LoggerFactory.getLogger(Eprint.class);

    private static final String IACR_EPRINT_EXP = "d{4}\\/\\d{3,5}";
    private static final String HTTP_EXP = "https:\\/\\/";
    private static final String URL_SHORT_EXP = "ia\\.cr\\/";
    private static final String URL_EXP = "eprint\\.iacr\\.org\\/";
    private final String iacrEprint;

    IacrEprint(String iacrEprint) {
        Objects.requireNonNull(iacrEprint);

        String trimmedId = iacrEprint.trim();

        if (matchesExcepted(trimmedId)) {
            Matcher matcher = Pattern.compile(IACR_EPRINT_EXP).matcher(trimmedId);
            this.iacrEprint = matcher.group(1);
        } else {
            throw new IllegalArgumentException(trimmedId + " is not a valid IacrEprint identifier.");
        }

    }

    private boolean matchesExcepted(String identifier) {
        return iacrEprint.matches(IACR_EPRINT_EXP) ||
            iacrEprint.matches(URL_EXP + IACR_EPRINT_EXP) ||
            iacrEprint.matches(URL_SHORT_EXP + IACR_EPRINT_EXP) ||
            iacrEprint.matches(HTTP_EXP + URL_EXP + IACR_EPRINT_EXP) ||
            iacrEprint.matches(HTTP_EXP + URL_SHORT_EXP + IACR_EPRINT_EXP);
    }

    public static Optional<IacrEprint> parse(String identifier) {
        String trimmed = identifier.strip();
        try {
            return Optional.of(new IacrEprint(trimmed));
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }

    @Override
    public String getNormalized() {
        return iacrEprint;
    }

    @Override
    public Field getDefaultField() {
        return StandardField.EPRINT;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            URI uri = new URI(RESOLVER.getScheme(), RESOLVER.getHost(), "/" + iacrEprint, null);
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            // should never happen
            LOGGER.error(iacrEprint + " could not be encoded as URI.", e);
            return Optional.empty();
        }
    }

}
