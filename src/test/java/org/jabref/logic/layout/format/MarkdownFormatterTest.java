package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarkdownFormatterTest {

    private MarkdownFormatter markdownFormatter;

    @BeforeEach
    void setUp() {
        markdownFormatter = new MarkdownFormatter();
    }

    @Test
    void formatWhenFormattingPlainTextThenReturnsTextWrappedInParagraph() {
        assertEquals("<p>Hello World</p>", markdownFormatter.format("Hello World"));
    }

    @Test
    void formatWhenFormattingHeaderThenReturnsHeaderInHtml() {
        assertEquals("<h1>Hello World</h1>", markdownFormatter.format("# Hello World"));
    }

    @Test
    void formatWhenFormattingBoldTextThenReturnsBoldTextInHtml() {
        assertEquals("<p><strong>Hello World</strong></p>", markdownFormatter.format("**Hello World**"));
    }

    @Test
    void formatWhenFormattingItalicTextThenReturnsItalicTextInHtml() {
        assertEquals("<p><em>Hello World</em></p>", markdownFormatter.format("*Hello World*"));
    }

    @Test
    void formatWhenFormattingLinkThenReturnsLinkInHtml() {
        assertEquals("<p><a href=\"https://example.com\">Example</a></p>", markdownFormatter.format("[Example](https://example.com)"));
    }

    @Test
    void formatWhenFormattingImageThenReturnsImageInHtml() {
        assertEquals("<p><img src=\"https://example.com/image.jpg\" alt=\"Example Image\" /></p>", markdownFormatter.format("![Example Image](https://example.com/image.jpg)"));
    }

    @Test
    void formatWhenFormattingBlockquoteThenReturnsBlockquoteInHtml() {
        assertEquals("<blockquote> <p>Hello World</p> </blockquote>", markdownFormatter.format("> Hello World"));
    }

    @Test
    void formatWhenFormattingCodeBlockThenReturnsCodeBlockInHtml() {
        assertEquals("<pre><code>Hello World </code></pre>", markdownFormatter.format("```\nHello World\n```"));
    }

    @Test
    void formatWhenFormattingComplexMarkupThenReturnsOnlyOneLine() {
        assertFalse(markdownFormatter.format("Markup\n\n* list item one\n* list item 2\n\n rest").contains("\n"));
    }

    @Test
    void formatWhenFormattingEmptyStringThenReturnsEmptyString() {
        assertEquals("", markdownFormatter.format(""));
    }

    @Test
    void formatWhenFormattingNullThenThrowsException() {
        Exception exception = assertThrows(NullPointerException.class, () -> markdownFormatter.format(null));
        assertEquals("Field Text should not be null, when handed to formatter", exception.getMessage());
    }

    @Test
    void formatWhenFormattingStringWithBracesThenKeepBraces() {
        assertEquals("<p>{Hello World}</p>", markdownFormatter.format("{Hello World}"));
    }

    @Test
    void formatWhenFormattingQuotesRemovesNewLines() {
        assertEquals(
                "<pre><code class=\"language-javascript\">function foo() {     return 'bar'; } </code></pre>",
                markdownFormatter.format(
                """
                ```javascript
                function foo() {
                    return 'bar';
                }
                ```"""
                )
        );
    }
}
