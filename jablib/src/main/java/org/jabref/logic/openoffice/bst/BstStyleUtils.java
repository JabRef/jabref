package org.jabref.logic.openoffice.bst;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

/// Utility methods for processing HTML produced by pandoc into a format suitable for insertion
/// into a LibreOffice document via [OOTextIntoOO].
///
/// This is the BST counterpart of [CSLFormatUtils]: it contains only the transforms that apply
/// to pandoc HTML. CSL-exclusive operations (the `csl-left-margin`/`csl-right-inline` div merge,
/// and the citeproc-java `<span style="font-weight/style/decoration/variant:…">` conversions)
/// are intentionally absent - pandoc never produces those structures.
///
/// Pandoc-specific tag conversions (`<em>`, `<strong>`, `<span class="smallcaps">`) are handled
/// upstream in [BstHtmlToOOText] before this class is called.
@NullMarked
public final class BstStyleUtils {

    private BstStyleUtils() {
    }

    /// Transforms pandoc HTML into a form that [OOTextIntoOO] can write into a LibreOffice document.
    ///
    /// Steps:
    /// 1. Decode HTML entities (`&amp;` → `&`, `&#x201C;` → `"`, etc.)
    /// 2. Strip `<div>` tags (pandoc may emit them for block-level content)
    /// 3. Strip hyperlinks (`<a>` tags - LibreOffice OOText does not support arbitrary links)
    /// 4. Strip any remaining `<span>` tags (pandoc-specific ones are already converted by [BstHtmlToOOText])
    /// 5. Normalise line breaks to `<p></p>` paragraph separators
    /// 6. Remove a leading empty paragraph (produced by step 5 when the text starts with a newline)
    /// 7. Collapse multiple consecutive trailing `<p></p>` into one
    /// 8. Trim surrounding whitespace
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
        // by BstHtmlToOOText before this method is called)
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
