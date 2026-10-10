package org.jabref.gui.bibtexhighlighter;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibTeXSyntaxHtmlExporterTest {

    private BibTeXSyntaxHighlighter syntaxHighlighter;

    @BeforeEach
    void setUp() {
        syntaxHighlighter = new BibTeXSyntaxHighlighter();
    }

    @Test
    void toHtmlReturnsEmptyStringForEmptyInput() {
        assertEquals("", BibTeXSyntaxHtmlExporter.toHtml("", syntaxHighlighter));
    }

    // [utest->req~entry-editor.source-tab.copy-syntax-highlighting~1]
    @Test
    void toHtmlPreservesBibTeXStructureAndAddsSyntaxSpans() {
        String bibtex = """
                @Article{Agrawal2015,
                  author = {Agrawal, Shikha},
                  title = {Survey on Anomaly Detection},
                  year = {2015},
                }
                """;

        String html = BibTeXSyntaxHtmlExporter.toHtml(bibtex, syntaxHighlighter);

        assertTrue(html.startsWith("<!DOCTYPE html>"), "Should be a complete HTML document");
        assertTrue(html.contains("<pre style=\"font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace; font-size: 10pt; line-height: 1.4; color: #24292e;\">"));
        assertTrue(html.contains("<span style=\"color: #0033b3; font-weight: bold;\">@Article</span>"));
        assertTrue(html.contains("<span style=\"color: #177534; font-weight: bold;\">Agrawal2015</span>"));
        assertTrue(html.contains("<span style=\"color: #b05a00;\">author</span>"));
        assertTrue(html.contains("<span style=\"color: #b05a00;\">title</span>"));
        assertTrue(html.contains("<span style=\"color: #b05a00;\">year</span>"));
        assertTrue(html.contains("<span style=\"color: #067d17;\">{Agrawal, Shikha}</span>"));
        assertTrue(html.contains("<span style=\"color: #067d17;\">{Survey on Anomaly Detection}</span>"));
        assertTrue(html.contains("<span style=\"color: #067d17;\">{2015}</span>"));
        assertTrue(html.endsWith("</html>" + org.jabref.logic.os.OS.NEWLINE));
    }

    @Test
    void toHtmlEscapesHtmlSpecialCharacters() {
        String bibtex = """
                @Misc{testKey,
                  title = {Tom & Jerry <cartoon> "rocks"},
                }
                """;

        String html = BibTeXSyntaxHtmlExporter.toHtml(bibtex, syntaxHighlighter);

        assertTrue(html.contains("Tom &amp; Jerry"));
        assertTrue(html.contains("&lt;cartoon&gt;"));
        assertTrue(html.contains("&quot;rocks&quot;"));
        assertFalse(html.contains("<cartoon>"));
    }

    @Test
    void fromCategoryNameReturnsDefaultOnUnknownCategory() {
        assertEquals(BibTeXStyleClass.DEFAULT, BibTeXStyleClass.fromCategoryName("NON_EXISTENT_CATEGORY"));
        assertEquals(BibTeXStyleClass.KEYWORD, BibTeXStyleClass.fromCategoryName("KEYWORD"));
        assertEquals(BibTeXStyleClass.STRING, BibTeXStyleClass.fromCategoryName("STRING"));
    }

    @Test
    void allStyleClassesHaveDefinedInlineCss() {
        for (BibTeXStyleClass styleClass : BibTeXStyleClass.values()) {
            if (styleClass != BibTeXStyleClass.DEFAULT) {
                assertFalse(styleClass.getInlineCss().isBlank(),
                        () -> "Style class " + styleClass + " should have defined inline CSS");
            } else {
                assertEquals("", styleClass.getInlineCss());
            }
        }
    }
}
