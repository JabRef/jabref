package org.jabref.model.openoffice.ootext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class OOFormatTest {

    @ParameterizedTest
    @NullAndEmptySource
    void paragraphOmitsStyleForNullOrEmptyStyle(@Nullable String paraStyle) {
        OOText text = OOText.fromString("<b>Citation</b>");

        assertEquals("<p><b>Citation</b></p>", OOFormat.paragraph(text, paraStyle).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\t", "\n", " \t\n"})
    void paragraphPreservesWhitespaceStyle(String paraStyle) {
        OOText text = OOText.fromString("<b>Citation</b>");

        assertEquals("<p oo:ParaStyleName=\"" + paraStyle + "\"><b>Citation</b></p>",
                OOFormat.paragraph(text, paraStyle).toString());
    }

    @Test
    void paragraphUsesNamedStyle() {
        OOText text = OOText.fromString("<b>Citation</b>");

        assertEquals("<p oo:ParaStyleName=\"Bibliography\"><b>Citation</b></p>",
                OOFormat.paragraph(text, "Bibliography").toString());
    }

    @Test
    void paragraphPreservesWhitespaceAroundNamedStyle() {
        OOText text = OOText.fromString("<b>Citation</b>");

        assertEquals("<p oo:ParaStyleName=\" Bibliography \"><b>Citation</b></p>",
                OOFormat.paragraph(text, " Bibliography ").toString());
    }
}
