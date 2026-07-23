package org.jabref.logic.openoffice.bst;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

/// Utilities shared across BST-based formatting pipelines (Preview and LibreOffice).
///
/// - Legacy LaTeX -> modern commands that pandoc recognizes
/// - Mapping pandoc HTML spans to OOText inline tags
/// - Mapping inline LaTeX formatting to HTML (Preview path)
/// - Generic HTML cleanup and entity decoding
@NullMarked
public final class BSTFormatUtils {

    private BSTFormatUtils() {}

    // ---- Pandoc HTML -> OOText full conversion (wrapper used by LO path) ----
    public static String convertPandocHtmlToOOText(String pandocHtml) {
        String s = pandocHtml.trim();
        // Unwrap outer <p>…</p>; internal paragraph boundaries become <p></p>
        s = s.replaceAll("(?s)</p>\\s*<p>", "<p></p>");
        s = s.replaceAll("(?s)^<p>", "");
        s = s.replaceAll("(?s)</p>$", "");
        // Map pandoc spans/elements to OOText inline tags
        s = mapPandocInlineToOO(s);
        // Delegate generic cleanup and entity decoding
        return transformHTML(s);
    }

    // ---- Pre-normalization for pandoc ----

    public static String normalizeLegacyForPandoc(String latex) {
        String s = latex;
        s = replaceLegacySwitch(s, "sc", "textsc");
        s = replaceLegacySwitch(s, "bf", "textbf");
        s = replaceLegacySwitch(s, "it", "textit");
        s = replaceLegacySwitch(s, "em", "emph");
        return s;
    }

    private static String replaceLegacySwitch(String input, String legacy, String modern) {
        String needle = "{\\" + legacy; // e.g., "{\\sc"
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            int j = input.indexOf(needle, i);
            if (j < 0) {
                out.append(input, i, input.length());
                break;
            }
            out.append(input, i, j);
            int k = j + needle.length();
            // skip whitespace after the legacy command
            int ws = k;
            while (ws < input.length() && Character.isWhitespace(input.charAt(ws))) {
                ws++;
            }
            int contentStart = ws;
            int depth = 0;
            int pos = ws;
            boolean closed = false;
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    if (depth == 0) {
                        String content = input.substring(contentStart, pos);
                        out.append('\\').append(modern).append('{').append(content).append('}');
                        i = pos + 1;
                        closed = true;
                        break;
                    } else {
                        depth--;
                    }
                }
                pos++;
            }
            if (!closed) {
                out.append(input, j, input.length());
                break;
            }
        }
        return out.toString();
    }

    // ---- Pandoc HTML -> OOText inline mapping (LibreOffice path) ----
    // Most of the lines below are based on trial-and-error
    // TODO: Optimize and remove whatever is not needed

    public static String mapPandocInlineToOO(String html) {
        String s = html;
        // element tags
        s = s.replaceAll("(?s)<em>(.*?)</em>", "<i>$1</i>");
        s = s.replaceAll("(?s)<strong>(.*?)</strong>", "<b>$1</b>");
        // style/class spans
        s = s.replaceAll("(?s)<span\\s+class=\"[^\"]*smallcaps[^\"]*\"[^>]*>(.*?)</span>", "<smallcaps>$1</smallcaps>");
        s = s.replaceAll("(?s)<span\\s+style=\"[^\"]*font-variant\\s*:\\s*small-caps[^\"]*\"[^>]*>(.*?)</span>", "<smallcaps>$1</smallcaps>");
        s = s.replaceAll("(?s)<span\\s+style=\"[^\"]*font-weight\\s*:\\s*bold[^\"]*\"[^>]*>(.*?)</span>", "<b>$1</b>");
        s = s.replaceAll("(?s)<span\\s+style=\"[^\"]*font-style\\s*:\\s*italic[^\"]*\"[^>]*>(.*?)</span>", "<i>$1</i>");
        s = s.replaceAll("(?s)<span\\s+style=\"[^\"]*text-decoration\\s*:\\s*underline[^\"]*\"[^>]*>(.*?)</span>", "<u>$1</u>");
        return s;
    }

    // ---- Inline LaTeX -> HTML mapping (Preview path) ----

    public static String mapInlineLatexToHtml(String latex) {
        String s = latex;
        s = s.replaceAll("\\\\emph\\{([^}]*?)}", "<i>$1</i>");
        s = s.replaceAll("\\\\textit\\{([^}]*?)}", "<i>$1</i>");
        s = s.replaceAll("\\\\textbf\\{([^}]*?)}", "<b>$1</b>");
        s = s.replaceAll("\\{\\\\em\\s+([^}]*?)}", "<i>$1</i>");
        s = s.replaceAll("\\{\\\\it\\s+([^}]*?)}", "<i>$1</i>");
        s = s.replaceAll("\\{\\\\bf\\s+([^}]*?)}", "<b>$1</b>");
        // small caps: \textsc{X} and {\sc X}
        s = s.replaceAll("\\\\textsc\\{([^}]*?)}", "<span style=\"font-variant: small-caps\">$1</span>");
        s = s.replaceAll("\\{\\\\sc\\s+([^}]*?)}", "<span style=\"font-variant: small-caps\">$1</span>");
        return s;
    }

    // ---- Generic HTML cleanup and entity decoding ----

    public static String transformHTML(String html) {
        // Decode HTML entities (&amp;, &#x201C;, &nbsp;, etc.)
        html = StringEscapeUtils.unescapeHtml4(html);

        // Strip <div> tags (pandoc emits them for block-level content such as block quotes)
        html = html.replaceAll("<div[^>]*>", "");
        html = html.replace("</div>", "");

        // Strip hyperlinks - LibreOffice OOText does not support arbitrary <a> links
        html = html.replaceAll("<a[^>]*>", "");
        html = html.replace("</a>", "");

        // Strip remaining <span> tags (pandoc-specific semantic tags are already handled
        // earlier by mapPandocInlineToOO before this method is called)
        html = html.replaceAll("</?span[^>]*>", "");

        // Convert line breaks to OOText paragraph separators
        html = html.replaceAll("[\n\r]+", "<p></p>");

        // Remove a leading empty paragraph separator
        html = html.replaceAll("^\\s*<p>\\s*</p>", "");

        // Collapse two or more consecutive trailing paragraph separators into one
        html = html.replaceAll("(?:<p>\\s*</p>\\s*){2,}$", "<p></p>");

        return html.trim();
    }
}
