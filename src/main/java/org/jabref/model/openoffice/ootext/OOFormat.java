package org.jabref.model.openoffice.ootext;

import org.jabref.model.strings.StringUtil;

/**
 * Helper functions to produce some of the markup as understood by OOTextIntoOO.write
 *
 * These do not cover all tags, only those needed to embed markup
 * from Layout and citation marker formatters into citation markers and
 * bibliography.
 */
public class OOFormat {

    private OOFormat() {
        /* */
    }

    /**
     * Mark {@code ootext} as using a character locale known to OO.
     *
     * @param locale language[-country[-territory]]
     *
     * https://www.openoffice.org/api/docs/common/ref/com/sun/star/lang/Locale.html
     *
     * The country part is optional.
     *
     * The territory part is not only optional, the allowed "codes are vendor and browser-specific",
     * so probably best to avoid them if possible.
     *
     */
    public static OOText setLocale(OOText ootext, String locale) {
        return OOText.fromString(String.format("<span lang=\"%s\">", locale) + ootext.toString() + "</span>");
    }

    /**
     * Mark {@code ootext} as using the character locale "zxx", which means "no language", "no
     * linguistic content".
     *
     * Used around citation marks, probably to turn off spellchecking.
     *
     */
    public static OOText setLocaleNone(OOText ootext) {
        return OOFormat.setLocale(ootext, "zxx");
    }

    /**
     * Mark {@code ootext} using a character style {@code charStyle}
     *
     * @param charStyle Name of a character style known to OO. May be empty for "Standard", which in
     *                  turn means do not override any properties.
     *
     */
    public static OOText setCharStyle(OOText ootext, String charStyle) {
        return OOText.fromString(String.format("<span oo:CharStyleName=\"%s\">", charStyle)
                                 + ootext.toString()
                                 + "</span>");
    }

    /**
     * Mark {@code ootext} as part of a paragraph with style {@code paraStyle}
     */
    public static OOText paragraph(OOText ootext, String paraStyle) {
        if (StringUtil.isNullOrEmpty(paraStyle)) {
            return paragraph(ootext);
        }
        String startTag = String.format("<p oo:ParaStyleName=\"%s\">", paraStyle);
        return OOText.fromString(startTag + ootext.toString() + "</p>");
    }

    /**
     * Mark {@code ootext} as part of a paragraph.
     */
    public static OOText paragraph(OOText ootext) {
        return OOText.fromString("<p>" + ootext.toString() + "</p>");
    }

    /**
     * Format an OO cross-reference showing the target's page number as label to a reference mark.
     */
    public static OOText formatReferenceToPageNumberOfReferenceMark(String referenceMarkName) {
        String string = String.format("<oo:referenceToPageNumberOfReferenceMark target=\"%s\">", referenceMarkName);
        return OOText.fromString(string);
    }
 }
