package org.jabref.logic.openoffice.bst;

import org.jabref.logic.openoffice.oocsltext.CSLFormatUtils;

import org.jspecify.annotations.NullMarked;

/// Normalizes pandoc's HTML output into the dialect that [CSLFormatUtils] understands,
/// then delegates to it.
///
/// The critical difference from CSL: pandoc wraps every paragraph in `<p>…</p>` rather than
/// `<div>…</div>`. [CSLFormatUtils.transformHTML] strips `<div>` but **not** `<p>content</p>`,
/// so we must unwrap the outer paragraph wrapper here before delegating — otherwise
/// `OOTextIntoOO` turns `<p>` into a hard paragraph break that orphans the `"[n] "` prefix.
@NullMarked
public final class BstHtmlToOOText {

    private BstHtmlToOOText() {
    }

    /// Converts pandoc HTML to an OOText-compatible string.
    ///
    /// - Strips pandoc's outer `<p>…</p>` wrapper (leaving content as an inline run).
    /// - Converts internal paragraph boundaries to `<p></p>` (empty-paragraph separator).
    /// - Maps `<em>` → `<i>`, `<strong>` → `<b>`, small-caps spans.
    /// - Delegates the rest to [CSLFormatUtils.transformHTML].
    public static String convert(String pandocHtml) {
        String s = pandocHtml.trim();

        // Convert internal paragraph boundaries to the empty-paragraph separator that
        // transformHTML understands, then strip the outer opening and closing wrappers.
        s = s.replaceAll("(?s)</p>\\s*<p>", "<p></p>");
        s = s.replaceAll("(?s)^<p>", "");
        s = s.replaceAll("(?s)</p>$", "");

        s = s.replaceAll("(?s)<em>(.*?)</em>", "<i>$1</i>");
        s = s.replaceAll("(?s)<strong>(.*?)</strong>", "<b>$1</b>");
        s = s.replaceAll("(?s)<span class=\"smallcaps\">(.*?)</span>", "<smallcaps>$1</smallcaps>");
        return CSLFormatUtils.transformHTML(s);
    }
}
