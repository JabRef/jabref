package org.jabref.logic.openoffice.bst;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern INLINE_MATH_SPAN = Pattern.compile("(?s)<span\\s+class=\\\"math inline\\\"[^>]*>(.*?)</span>");
    private static final Pattern BRACED_ETALCHAR_PATTERN = Pattern.compile("\\{\\\\etalchar\\{([^}]*)}}");
    private static final Pattern ETALCHAR_PATTERN = Pattern.compile("\\\\etalchar\\{([^}]*)}");
    private BSTFormatUtils() {
    }

    // ---- Pandoc HTML -> OOText full conversion (wrapper used by LO path) ----
    public static String convertPandocHtmlToOOText(String pandocHtml) {
        String s = pandocHtml.trim();
        // Unwrap outer <p>…</p>; internal paragraph boundaries become <p></p>
        s = s.replaceAll("(?s)</p>\\s*<p>", "<p></p>");
        s = s.replaceAll("(?s)^<p>", "");
        s = s.replaceAll("(?s)</p>$", "");
        // Strip emphasis inside inline math spans only, then drop the math wrapper
        s = stripEmphasisInsideInlineMath(s);
        // Map pandoc spans/elements to OOText inline tags
        s = mapPandocInlineToOO(s);
        // Delegate generic cleanup and entity decoding
        return transformHTML(s);
    }

    // Removes <em>/<strong> only inside <span class="math inline">…</span> and inlines the content
    // Needed because pandoc wraps inline math identifiers in <em>
    // (e.g., <span class="math inline"><em>Σ</em></span>), which would otherwise render Greek
    // letters italic in LibreOffice contrary to the desired bibliography styling.
    private static String stripEmphasisInsideInlineMath(String html) {
        Matcher matcher = INLINE_MATH_SPAN.matcher(html);
        StringBuilder out = new StringBuilder(html.length());
        while (matcher.find()) {
            String inner = matcher.group(1);
            String cleaned = inner.replaceAll("(?s)</?em[^>]*>", "");
            cleaned = cleaned.replaceAll("(?s)</?strong[^>]*>", "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(cleaned));
        }
        matcher.appendTail(out);
        return out.toString();
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

    /// Normalizes a BST `\\bibitem[...]` label by replacing supported label-only helper macros
    /// with their plain-text equivalents.
    ///
    /// Currently this converts alpha-style `\\etalchar{...}` markers such as
    /// `TLY{\\etalchar{+}}21` to `TLY+21` so the label can be shown directly in previews and
    /// LibreOffice citations.
    public static String normalizeBibItemLabel(String label) {
        String normalized = BRACED_ETALCHAR_PATTERN.matcher(label).replaceAll("$1");
        return ETALCHAR_PATTERN.matcher(normalized).replaceAll("$1");
    }

    public static String convertInlineLatexFormattingToHtml(String latex) {
        String html = replaceCommandWithBalancedArgument(latex, "textsc", "<span style=\"font-variant: small-caps\">", "</span>");
        html = replaceLegacySwitchWithBalancedArgument(html, "sc", "<span style=\"font-variant: small-caps\">", "</span>");
        html = replaceCommandWithBalancedArgument(html, "textsuperscript", "<sup>", "</sup>");
        return replaceCommandWithBalancedArgument(html, "textsubscript", "<sub>", "</sub>");
    }

    private static String replaceCommandWithBalancedArgument(String input, String command, String openingTag, String closingTag) {
        String needle = "\\" + command + "{";
        StringBuilder out = new StringBuilder(input.length());
        int i = 0;
        while (i < input.length()) {
            int j = input.indexOf(needle, i);
            if (j < 0) {
                out.append(input, i, input.length());
                break;
            }
            out.append(input, i, j);
            int contentStart = j + needle.length();
            int groupEnd = findBalancedGroupEnd(input, contentStart);
            if (groupEnd < 0) {
                out.append(input, j, input.length());
                break;
            }
            out.append(openingTag).append(input, contentStart, groupEnd).append(closingTag);
            i = groupEnd + 1;
        }
        return out.toString();
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
            // Skip whitespace after the legacy command
            int whitespacePos = k;
            while (whitespacePos < input.length() && Character.isWhitespace(input.charAt(whitespacePos))) {
                whitespacePos++;
            }
            int contentStart = whitespacePos;
            int groupEnd = findBalancedGroupEnd(input, contentStart);
            if (groupEnd < 0) {
                out.append(input, j, input.length());
                break;
            }
            out.append('\\').append(modern).append('{').append(input, contentStart, groupEnd).append('}');
            i = groupEnd + 1;
        }
        return out.toString();
    }

    private static String replaceLegacySwitchWithBalancedArgument(String input, String legacy, String openingTag, String closingTag) {
        String needle = "{\\" + legacy;
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
            int whitespacePos = k;
            while (whitespacePos < input.length() && Character.isWhitespace(input.charAt(whitespacePos))) {
                whitespacePos++;
            }
            int groupEnd = findBalancedGroupEnd(input, whitespacePos);
            if (groupEnd < 0) {
                out.append(input, j, input.length());
                break;
            }
            out.append(openingTag).append(input, whitespacePos, groupEnd).append(closingTag);
            i = groupEnd + 1;
        }
        return out.toString();
    }

    private static int findBalancedGroupEnd(String input, int contentStart) {
        int depth = 0;
        int pos = contentStart;
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                if (depth == 0) {
                    return pos;
                }
                depth--;
            }
            pos++;
        }
        return -1;
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
