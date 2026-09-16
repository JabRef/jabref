package org.jabref.gui.util.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.kusoroadeolu.veneer.JSONLexer;
import io.github.kusoroadeolu.veneer.JSONParser;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Splits JSON text into styled segments, so that AI answers containing JSON can be rendered
/// with syntax highlighting. Lexing and parsing are delegated to the Veneer grammar that is
/// also used for the BibTeX source editor (see `org.jabref.gui.bibtexhighlighter`).
///
/// The colors for the style classes are defined in `jabref-base.css`.
@NullMarked
public class JsonHighlighter {

    private static final Set<String> LITERALS = Set.of("true", "false", "null");

    /// A piece of the original text together with the CSS style class it should be rendered with.
    /// `styleClass` is empty for text between tokens (whitespace).
    public record Segment(String text, String styleClass) {
    }

    private JsonHighlighter() {
    }

    /// Checks whether the given text is a complete JSON object or array.
    public static boolean isJson(String text) {
        String trimmed = text.strip();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return false;
        }

        ErrorFlag lexerErrors = new ErrorFlag();
        CommonTokenStream tokenStream = tokenStream(trimmed);
        ((JSONLexer) tokenStream.getTokenSource()).addErrorListener(lexerErrors);
        tokenStream.fill();
        // Characters the lexer cannot match are dropped silently, so they have to be checked for separately.
        if (lexerErrors.hasError) {
            return false;
        }

        JSONParser parser = new JSONParser(tokenStream);
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());
        try {
            parser.json();
        } catch (ParseCancellationException _) {
            return false;
        }
        return parser.getCurrentToken().getType() == Token.EOF;
    }

    /// Splits the given JSON text into segments carrying a style class each.
    public static List<Segment> tokenize(String json) {
        CommonTokenStream tokenStream = tokenStream(json);
        tokenStream.fill();

        List<Segment> segments = new ArrayList<>();
        List<Token> tokens = tokenStream.getTokens();
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
                segments.add(new Segment(json.substring(position, token.getStartIndex()), ""));
            }

            segments.add(new Segment(token.getText(), styleClassOf(token, nextTokenText(tokens, i))));
            position = token.getStopIndex() + 1;
        }

        if (position < json.length()) {
            segments.add(new Segment(json.substring(position), ""));
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

    /// Records whether the lexer stumbled over a character.
    private static final class ErrorFlag extends BaseErrorListener {
        private boolean hasError;

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, @Nullable Object offendingSymbol, int line, int charPositionInLine, String msg, @Nullable RecognitionException e) {
            hasError = true;
        }
    }

    private static CommonTokenStream tokenStream(String text) {
        JSONLexer lexer = new JSONLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        return new CommonTokenStream(lexer);
    }
}
