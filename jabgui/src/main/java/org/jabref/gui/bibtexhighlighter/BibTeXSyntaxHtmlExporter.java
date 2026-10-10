package org.jabref.gui.bibtexhighlighter;

import java.util.List;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.strings.StringUtil;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter.BibTeXHighlightRegion;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Exports BibTeX source code to formatted HTML with inline CSS styling, preserving syntax highlighting
/// when copied to the system clipboard and pasted into external rich-text applications (e.g. LibreOffice,
/// Microsoft Word, Google Docs).
@NullMarked
public final class BibTeXSyntaxHtmlExporter {

    private BibTeXSyntaxHtmlExporter() {
    }

    /// Converts the given BibTeX code to an HTML document with syntax highlighting inline styles.
    ///
    /// @param bibtexCode the BibTeX source text to format
    /// @param syntaxHighlighter the syntax highlighter to compute token regions
    /// @return a formatted HTML document string with inline syntax styles
    // [impl->req~entry-editor.source-tab.copy-syntax-highlighting~1]
    public static String toHtml(String bibtexCode, BibTeXSyntaxHighlighter syntaxHighlighter) {
        if (bibtexCode.isEmpty()) {
            return "";
        }

        List<BibTeXHighlightRegion> regions = syntaxHighlighter.computeHighlightRegions(bibtexCode);
        StringBuilder contentBuilder = new StringBuilder();

        int cursor = 0;
        int textLength = bibtexCode.length();

        for (BibTeXHighlightRegion region : regions) {
            int start = Math.max(0, Math.min(region.start(), textLength));
            int end = Math.max(start, Math.min(region.end(), textLength));

            if (end <= cursor) {
                continue;
            }
            if (start < cursor) {
                start = cursor;
            }

            if (start > cursor) {
                appendEscaped(contentBuilder, bibtexCode.substring(cursor, start), null);
            }

            if (end > start) {
                BibTeXStyleClass styleClass = BibTeXStyleClass.fromCategoryName(region.category().name());
                appendEscaped(contentBuilder, bibtexCode.substring(start, end), styleClass.getInlineCss());
                cursor = end;
            }
        }

        if (cursor < textLength) {
            appendEscaped(contentBuilder, bibtexCode.substring(cursor), null);
        }

        return "<!DOCTYPE html>" + OS.NEWLINE +
                "<html>" + OS.NEWLINE +
                "  <head>" + OS.NEWLINE +
                "    <meta charset=\"utf-8\">" + OS.NEWLINE +
                "  </head>" + OS.NEWLINE +
                "  <body>" + OS.NEWLINE +
                "    <pre style=\"font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace; font-size: 10pt; line-height: 1.4; color: #24292e;\">" +
                contentBuilder +
                "</pre>" + OS.NEWLINE +
                "  </body>" + OS.NEWLINE +
                "</html>" + OS.NEWLINE;
    }

    private static void appendEscaped(StringBuilder sb, String segment, @Nullable String inlineCss) {
        if (segment.isEmpty()) {
            return;
        }
        String escaped = escapeHtml(segment);
        if (StringUtil.isNotBlank(inlineCss)) {
            sb.append("<span style=\"").append(inlineCss).append("\">").append(escaped).append("</span>");
        } else {
            sb.append(escaped);
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
