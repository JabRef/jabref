package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;

import org.apache.commons.text.StringEscapeUtils;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class BibliographyText {

    private BibliographyText() {
    }

    static List<OOText> start(RenderOptions options) {
        List<OOText> paragraphs = new ArrayList<>();
        if (!options.bibliographyTitle().isEmpty()) {
            paragraphs.add(OOFormat.paragraph(OOText.fromString(StringEscapeUtils.escapeHtml4(options.bibliographyTitle())), options.bibliographyHeaderFormat()));
        }
        return paragraphs;
    }

    static OOText entry(String text, RenderOptions options) {
        String body = text.endsWith("<p></p>") ? text.substring(0, text.length() - "<p></p>".length()) : text;
        return OOFormat.paragraph(OOFormat.setLocaleNone(OOText.fromString(body)), options.bibliographyBodyFormat());
    }
}
