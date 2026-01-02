package org.jabref.model.entry.identifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identifier for the arXiv. See https://arxiv.org/help/arxiv_identifier
 */
@AllowedToUseLogic("Uses StringUtil temporarily")
public class ArXivIdentifier extends EprintIdentifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArXivIdentifier.class);

    private static final String ARXIV_PREFIX = "http(s)?://arxiv.org/(abs|html|pdf)/|arxiv|arXiv";
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("(" + ARXIV_PREFIX + ")?\\s?:?\\s?(?<id>\\d{4}\\.\\d{4,5})(v(?<version>\\d+))?\\s?(\\[(?<classification>\\S+)\\])?");
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
        String identifier = value.replace(" ", "");
        Matcher identifierMatcher = IDENTIFIER_PATTERN.matcher(identifier);
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

    /// ArXiv articles are automatically assigned DOIs starting with the prefix
    /// `10.48550/`, followed by the ArXiv ID (with the colon replaced by a period).
    ///
    /// More information:
    /// [new-arxiv-articles-are-now-automatically-assigned-dois](https://blog.arxiv.org/2022/02/17/new-arxiv-articles-are-now-automatically-assigned-dois/)
    public Optional<DOI> inferDOI() {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        return DOI.parse("10.48550/arxiv." + identifier);
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
    public String asString() {
        if (StringUtil.isNotBlank(version)) {
            return identifier + "v" + version;
        } else {
            return identifier;
        }
    }

    public String asStringWithoutVersion() {
        return identifier;
    }

    @Override
    public Optional<URI> getExternalURI() {
        try {
            return Optional.of(new URI("https://arxiv.org/abs/" + asString()));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    public static Optional<ArXivIdentifier> findInText(String text) {
        if (StringUtil.isBlank(text)) {
            return Optional.empty();
        }

        Optional<ArXivIdentifier> directParse = parse(text);
        if (directParse.isPresent()) {
            return directParse;
        }

        Matcher matcher = IDENTIFIER_PATTERN.matcher(text);
        if (matcher.find()) {
            return parse(matcher.group());
        }

        return Optional.empty();
    }
}
