package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompositeFormatTest {

    @Test
    void emptyComposite() {
        LayoutFormatter f = new CompositeFormat();
        assertEquals("No Change", f.format("No Change"));
    }

    @Test
    void arrayComposite() {
        LayoutFormatter f = new CompositeFormat(new LayoutFormatter[] {fieldText -> fieldText + fieldText,
                fieldText -> "A" + fieldText, fieldText -> "B" + fieldText});

        assertEquals("BAff", f.format("f"));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            John Flynn and Sabine Gartska , John Flynn and Sabine Gartska
            Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee , Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee
            """)
    void doubleComposite(String inputForFirst, String inputForComposite) {
        LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(), new NoSpaceBetweenAbbreviations());
        LayoutFormatter first = new AuthorOrgSci();
        LayoutFormatter second = new NoSpaceBetweenAbbreviations();

        assertEquals(second.format(first.format(inputForFirst)), f.format(inputForComposite));
    }
}
