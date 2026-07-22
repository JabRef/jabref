package org.jabref.logic.openoffice.bst;

import org.jabref.logic.openoffice.oocsltext.CSLFormatUtils;

import org.jspecify.annotations.NullMarked;

/// Normalizes pandoc's semantic HTML into the dialect that [CSLFormatUtils] already understands,
/// then delegates to it. `transformHTML` handles entity-decoding, `<div>`/`<a>`/`<span>` stripping,
/// and paragraph breaks — all reused, not reimplemented.
@NullMarked
public final class BstHtmlToOOText {

    private BstHtmlToOOText() {
    }

    public static String convert(String pandocHtml) {
        String s = pandocHtml;
        s = s.replaceAll("(?s)<em>(.*?)</em>", "<i>$1</i>");
        s = s.replaceAll("(?s)<strong>(.*?)</strong>", "<b>$1</b>");
        s = s.replaceAll("(?s)<span class=\"smallcaps\">(.*?)</span>", "<smallcaps>$1</smallcaps>");
        return CSLFormatUtils.transformHTML(s);
    }
}
