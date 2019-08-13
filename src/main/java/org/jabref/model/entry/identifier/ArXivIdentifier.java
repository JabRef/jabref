package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Identifier for the arXiv. See https://arxiv.org/help/arxiv_identifier
 */
public class ArXivIdentifier implements Identifier {

    private final String identifier;
    private final String classification;

    ArXivIdentifier(String identifier) {
        this(identifier, "");
    }

    ArXivIdentifier(String identifier, String classification) {
        this.identifier = identifier.trim();
        this.classification = classification.trim();
    }

    public static Optional<ArXivIdentifier> parse(String value) {
        Pattern identifierPattern = Pattern.compile("(arxiv|arXiv)?\\s?:?\\s?(?<id>\\d{4}.\\d{4,5}(v\\d+)?)\\s?(\\[(?<classification>\\S+)\\])?");
        Matcher identifierMatcher = identifierPattern.matcher(value);
        if (identifierMatcher.matches()) {
            String id = identifierMatcher.group("id");
            String classification = identifierMatcher.group("classification");
            if (classification == null) {
                classification = "";
            }
            return Optional.of(new ArXivIdentifier(id, classification));
        }

        Pattern oldIdentifierPattern = Pattern.compile("(arxiv|arXiv)?\\s?:?\\s?(?<id>(?<classification>[a-z\\-]+(\\.[A-Z]{2})?)/\\d{7})");
        Matcher oldIdentifierMatcher = oldIdentifierPattern.matcher(value);
        if (oldIdentifierMatcher.matches()) {
            String id = oldIdentifierMatcher.group("id");
            String classification = oldIdentifierMatcher.group("classification");
            return Optional.of(new ArXivIdentifier(id, classification));
        }

        Pattern urlPattern = Pattern.compile("(http://arxiv.org/abs/)(?<id>\\S+)");
        Matcher urlMatcher = urlPattern.matcher(value);
        if (urlMatcher.matches()) {
            String id = urlMatcher.group("id");
            return Optional.of(new ArXivIdentifier(id));
        }

        return Optional.empty();
    }

    public Optional<String> getClassification() {
        if (classification.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(classification);
        }
    }

    @Override
    public String toString() {
        return "ArXivIdentifier{" +
                "identifier='" + identifier + '\'' +
                ", classification='" + classification + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArXivIdentifier that = (ArXivIdentifier) o;
        return Objects.equals(identifier, that.identifier) &&
                Objects.equals(classification, that.classification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, classification);
    }

    @Override
    public Field getDefaultField() {
        return StandardField.EPRINT;
    }

    @Override
    public String getNormalized() {
        return identifier;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://arxiv.org/abs/" + identifier));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
