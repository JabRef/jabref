package org.jabref.gui.util.component;

import java.util.ArrayList;
import java.util.List;

import io.github.kusoroadeolu.veneer.JSONLexer;
import io.github.kusoroadeolu.veneer.JSONParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;
import org.jspecify.annotations.NullMarked;

/// Splits JSON into styled segments, so that JSON in AI answers can be rendered with syntax highlighting.
/// Finding and indenting the JSON is done beforehand by [org.jabref.logic.ai.chatting.util.JsonAnswerFormatter].
///
/// Highlighting reuses [Veneer](https://apidia.net/mvn/io.github.kusoroadeolu/veneer), which already
/// highlights the BibTeX source editor (see [org.jabref.gui.bibtexhighlighter.BibTeXHighlighter]).
/// Its `JSONSyntaxHighlighter` cannot be used directly, though:
///
/// * It is made for terminals: `highlight(String)` returns the text with ANSI escape codes and line
///   numbers. For BibTeX, Veneer offers `computeHighlightRegions(String)`, which JavaFX can style; there
///   is no such method for JSON.
/// * It neither validates nor formats. An answer has to be recognized as JSON first — often with an
///   explanation before or after it — and indented, because models tend to send everything on one line.
///
/// Therefore, only Veneer's JSON grammar (`JSONLexer` and `JSONParser`) is used here. The parse tree tells
/// keys from string values and `true`, `false`, `null` from punctuation, which the tokens alone do not.
///
/// The colors for the style classes are defined in `jabref-base.css`.
@NullMarked
public class JsonHighlighter {

    /// A piece of the original text together with the CSS style class it should be rendered with.
    /// `styleClass` is empty for text between tokens (whitespace).
    public record Segment(String text, String styleClass) {
    }

    private JsonHighlighter() {
    }

    /// Splits the given JSON text into segments carrying a style class each.
    public static List<Segment> tokenize(String json) {
        CommonTokenStream tokenStream = tokenStream(json);
        JSONParser parser = new JSONParser(tokenStream);
        parser.removeErrorListeners();

        List<Segment> segments = new ArrayList<>();
        // The lexer counts code points, `String` counts UTF-16 units: an emoji would shift all offsets.
        int[] codePoints = json.codePoints().toArray();
        int position = 0;

        for (ParseTree node : Trees.getDescendants(parser.json())) {
            if (!(node instanceof TerminalNode terminal) || (terminal.getSymbol().getType() == Token.EOF)) {
                continue;
            }
            Token token = terminal.getSymbol();

            // Whitespace is skipped by the lexer, so it has to be taken from the original text.
            if (token.getStartIndex() > position) {
                segments.add(new Segment(new String(codePoints, position, token.getStartIndex() - position), ""));
            }

            String styleClass = styleClassOf(terminal);
            if ("json-string".equals(styleClass)) {
                addStringSegments(segments, token.getText());
            } else {
                segments.add(new Segment(token.getText(), styleClass));
            }
            position = token.getStopIndex() + 1;
        }

        if (position < codePoints.length) {
            segments.add(new Segment(new String(codePoints, position, codePoints.length - position), ""));
        }

        return segments;
    }

    /// Splits a string value, so that a quotation inside it (`\"...\"`) gets a style class of its own.
    /// Answers often quote the paper that way.
    private static void addStringSegments(List<Segment> segments, String string) {
        int start = 0;
        boolean inQuotation = false;

        int i = 0;
        while (i < string.length()) {
            if (string.charAt(i) != '\\') {
                i++;
                continue;
            }
            // A backslash in a valid JSON string is always followed by the escaped character.
            if (string.charAt(i + 1) == '"') {
                int boundary = inQuotation ? i + 2 : i;
                addStringSegment(segments, string.substring(start, boundary), inQuotation);
                start = boundary;
                inQuotation = !inQuotation;
            }
            // The escape sequence is two characters long.
            i += 2;
        }

        addStringSegment(segments, string.substring(start), inQuotation);
    }

    private static void addStringSegment(List<Segment> segments, String text, boolean quotation) {
        if (!text.isEmpty()) {
            segments.add(new Segment(text, quotation ? "json-string-quotation" : "json-string"));
        }
    }

    /// The role of a token follows from the grammar rule it belongs to: the string of a `pair` is its name,
    /// and a token of a `value` that is neither string nor number is one of `true`, `false`, `null`.
    private static String styleClassOf(TerminalNode terminal) {
        return switch (terminal.getSymbol().getType()) {
            case JSONLexer.NUMBER ->
                    "json-number";
            case JSONLexer.STRING ->
                    terminal.getParent() instanceof JSONParser.PairContext ? "json-key" : "json-string";
            default ->
                    terminal.getParent() instanceof JSONParser.ValueContext ? "json-literal" : "json-punctuation";
        };
    }

    private static CommonTokenStream tokenStream(String text) {
        JSONLexer lexer = new JSONLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        return new CommonTokenStream(lexer);
    }
}
