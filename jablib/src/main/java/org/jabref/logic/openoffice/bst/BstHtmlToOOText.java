package org.jabref.logic.openoffice.bst;

import org.jabref.model.openoffice.ootext.OOTextIntoOO;

import org.jspecify.annotations.NullMarked;

/// Converts pandoc HTML output into a string that [OOTextIntoOO] can write into a LibreOffice document.
///
/// Handles the pandoc-specific semantic tags first, then delegates general HTML cleanup to
/// [BstStyleUtils.transformHTML]. This keeps a clean separation: pandoc tag mapping here,
/// general OOText preparation in [BstStyleUtils].
@NullMarked
public final class BstHtmlToOOText {

    private BstHtmlToOOText() {
    }

    /// Converts a pandoc HTML fragment to an OOText-compatible string.
    ///
    /// Processing order:
    /// 1. Strip pandoc's outer `<p>…</p>` wrapper so the content is an inline run -
    ///    internal paragraph boundaries become `<p></p>` separators.
    /// 2. Map pandoc's semantic tags to the inline tags [OOTextIntoOO] understands:
    ///    `<em>` → `<i>`, `<strong>` → `<b>`, small-caps span → `<smallcaps>`.
    /// 3. Delegate remaining general HTML cleanup to [BstStyleUtils.transformHTML].
    public static String convert(String pandocHtml) {
        String s = pandocHtml.trim();

        // Unwrap pandoc's outer <p>…</p>; internal paragraph boundaries become <p></p>.
        s = s.replaceAll("(?s)</p>\\s*<p>", "<p></p>");
        s = s.replaceAll("(?s)^<p>", "");
        s = s.replaceAll("(?s)</p>$", "");

        // Map pandoc semantic tags to the OOText inline tag set.
        // These are never produced by citeproc-java, so they belong here, not in BstStyleUtils.
        s = s.replaceAll("(?s)<em>(.*?)</em>", "<i>$1</i>");
        s = s.replaceAll("(?s)<strong>(.*?)</strong>", "<b>$1</b>");
        s = s.replaceAll("(?s)<span class=\"smallcaps\">(.*?)</span>", "<smallcaps>$1</smallcaps>");

        return BstStyleUtils.transformHTML(s);
    }
}
