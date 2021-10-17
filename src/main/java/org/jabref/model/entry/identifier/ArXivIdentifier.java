package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * Identifier for the arXiv. See https://arxiv.org/help/arxiv_identifier
 */
public class ArXivIdentifier implements Identifier {

    private static final String ARXIV_PREFIX = "http(s)?://arxiv.org/(abs|pdf)/|arxiv|arXiv";
    private final String identifier;
    private final String classification;
    private final String version;

    ArXivIdentifier(String identifier) {
        this(identifier, "", "");
    }

    ArXivIdentifier(String identifier, String classification) {
        this(identifier, "", classification);
    }

    ArXivIdentifier(String identifier, String version, String classification) {
        this.identifier = identifier.trim();
        this.version = version.trim();
        this.classification = classification.trim();
    }

    public static Optional<ArXivIdentifier> parse(String value) {
        String identifier = value.replaceAll(" ", "");
        Pattern identifierPattern = Pattern.compile("(" + ARXIV_PREFIX + ")?\\s?:?\\s?(?<id>\\d{4}\\.\\d{4,5})(v(?<version>\\d+))?\\s?(\\[(?<classification>\\S+)\\])?");
        Matcher identifierMatcher = identifierPattern.matcher(identifier);
        if (identifierMatcher.matches()) {
            return getArXivIdentifier(identifierMatcher);
        }

        Pattern oldIdentifierPattern = Pattern.compile("(" + ARXIV_PREFIX + ")?\\s?:?\\s?(?<id>(?<classification>[a-z\\-]+(\\.[A-Z]{2})?)/\\d{7})(v(?<version>\\d+))?");
        Matcher oldIdentifierMatcher = oldIdentifierPattern.matcher(identifier);
        if (oldIdentifierMatcher.matches()) {
            return getArXivIdentifier(oldIdentifierMatcher);
        }

        return Optional.empty();
    }

    private static Optional<ArXivIdentifier> getArXivIdentifier(Matcher matcher) {
        String id = matcher.group("id");
        String classification = matcher.group("classification");
        if (classification == null) {
            classification = "";
        }
        String version = matcher.group("version");
        if (version == null) {
            version = "";
        }
        return Optional.of(new ArXivIdentifier(id, version, classification));
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
        if (StringUtil.isNotBlank(version)) {
            return identifier + "v" + version;
        } else {
            return identifier;
        }
    }

    public String getNormalizedWithoutVersion() {
        return identifier;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://arxiv.org/abs/" + getNormalized()));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
