package org.jabref.gui.util.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.github.kusoroadeolu.veneer.JSONLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/// Formats and splits JSON text into styled segments, so that AI answers containing JSON can be
/// rendered readably and with syntax highlighting. Parsing and formatting are delegated to Jackson,
/// tokenizing to the Veneer grammar that is also used for the BibTeX source editor
/// (see `org.jabref.gui.bibtexhighlighter`).
///
/// The colors for the style classes are defined in `jabref-base.css`.
@NullMarked
public class JsonHighlighter {

    private static final Set<String> LITERALS = Set.of("true", "false", "null");

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonHighlighter.class);

    /// Duplicate names make the parse fail, because the tree would silently drop the first value.
    /// Decimals are kept as [java.math.BigDecimal], so that no digits are lost on the way out.
    private static final JsonMapper MAPPER = JsonMapper.builder()
                                                       .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                                                       .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                                                       .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                                                       .enable(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                                                       .build();

    private static final ObjectWriter WRITER = MAPPER.writer().with(prettyPrinter());

    /// A piece of the original text together with the CSS style class it should be rendered with.
    /// `styleClass` is empty for text between tokens (whitespace).
    public record Segment(String text, String styleClass) {
    }

    private JsonHighlighter() {
    }

    /// Returns the given text as indented JSON, or empty if it is not a JSON object or array.
    public static Optional<String> prettyPrint(String text) {
        String trimmed = text.strip();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return Optional.empty();
        }

        try {
            return Optional.of(WRITER.writeValueAsString(MAPPER.readTree(trimmed)));
        } catch (JacksonException e) {
            LOGGER.debug("Text starting like JSON could not be formatted as JSON", e);
            return Optional.empty();
        }
    }

    /// Splits the given JSON text into segments carrying a style class each.
    public static List<Segment> tokenize(String json) {
        CommonTokenStream tokenStream = tokenStream(json);
        tokenStream.fill();

        List<Segment> segments = new ArrayList<>();
        List<Token> tokens = tokenStream.getTokens();
        // The lexer counts code points, `String` counts UTF-16 units: an emoji would shift all offsets.
        int[] codePoints = json.codePoints().toArray();
        int position = 0;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() == Token.EOF) {
                break;
            }
            if (isWhitespace(token)) {
                continue;
            }

            // Whitespace is skipped by the lexer, so it has to be taken from the original text.
            if (token.getStartIndex() > position) {
                segments.add(new Segment(new String(codePoints, position, token.getStartIndex() - position), ""));
            }

            segments.add(new Segment(token.getText(), styleClassOf(token, nextTokenText(tokens, i))));
            position = token.getStopIndex() + 1;
        }

        if (position < codePoints.length) {
            segments.add(new Segment(new String(codePoints, position, codePoints.length - position), ""));
        }

        return segments;
    }

    private static boolean isWhitespace(Token token) {
        return (token.getType() == JSONLexer.WS) || (token.getType() == JSONLexer.NEWLINE);
    }

    /// The text of the next token that is not whitespace, or the empty string behind the last token.
    private static String nextTokenText(List<Token> tokens, int index) {
        for (int i = index + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (!isWhitespace(token)) {
                return token.getText();
            }
        }
        return "";
    }

    private static String styleClassOf(Token token, String nextTokenText) {
        return switch (token.getType()) {
            case JSONLexer.NUMBER ->
                    "json-number";
            case JSONLexer.STRING ->
                    ":".equals(nextTokenText) ? "json-key" : "json-string";
            default ->
                    LITERALS.contains(token.getText()) ? "json-literal" : "json-punctuation";
        };
    }

    /// Two spaces per level for objects and arrays alike, and no space in front of the colon.
    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        return new DefaultPrettyPrinter()
                .withObjectIndenter(indenter)
                .withArrayIndenter(indenter)
                .withSeparators(Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER));
    }

    private static CommonTokenStream tokenStream(String text) {
        JSONLexer lexer = new JSONLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        return new CommonTokenStream(lexer);
    }
}
