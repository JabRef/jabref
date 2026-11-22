package org.jabref.logic.layout.format;

import java.util.regex.Pattern;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownEscaperTest {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final MarkdownEscaper markdownEscaper = new MarkdownEscaper();
    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '"', textBlock = """
            ""                          | ""
            "Plain title"               | "Plain title"
            "Title, with: punctuation." | "Title, with: punctuation."
            "*bold* and _italic_"       | "\\*bold\\* and \\_italic\\_"
            "[link](target)"            | "\\[link\\](target)"
            "<b>html</b>"               | "\\<b>html\\</b>"
            "`code` a|b #1 back\\\\slash" | "\\`code\\` a\\|b \\#1 back\\\\slash"
            "Multi\n  line\ttitle  "    | "Multi line title"
            """)
    void format(String input, String expected) {
        assertEquals(expected, markdownEscaper.format(input.translateEscapes()));
    }

    /// Round trip: a CommonMark parser must read the escaped text back as the original literal text, without any markup.
    @ParameterizedTest
    @ValueSource(strings = {
            "Plain title",
            "Title, with: punctuation.",
            "*bold* and **strong** and _italic_ and __strong__",
            "[link](https://example.org) and [ref][1] and ![image](a.png)",
            "<b>html</b> and <https://example.org> and <!-- comment -->",
            "`code` and ``double`` code",
            "a|b|c",
            "# Heading",
            "Hash # in the middle",
            "back\\slash and \\*escaped\\*",
            "Trailing backslash\\",
            "Line\nbreak  and\ttab",
            "1. Ordered list marker",
            "- Bullet marker",
            "+ Plus marker",
            "> Quote marker",
            "Entity &copy; and &amp; and &#169;",
            "~~strike~~ and http://auto.link",
            "===",
            "---"
    })
    void escapedTextIsParsedBackAsLiteralText(String input) {
        String literalText = WHITESPACE.matcher(input).replaceAll(" ").strip();
        String expectedHtml = "<p>" + escapeHtml(literalText) + "</p>\n";

        assertEquals(expectedHtml, renderer.render(parser.parse(markdownEscaper.format(input))));
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
