package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class CompositeFormatTest {

    @Test
    public void testEmptyComposite() {
        LayoutFormatter f = new CompositeFormat();
        Assert.assertEquals("No Change", f.format("No Change"));
    }

    @Test
    public void testArrayComposite() {
        LayoutFormatter f = new CompositeFormat(new LayoutFormatter[] {fieldText -> fieldText + fieldText,
                fieldText -> "A" + fieldText, fieldText -> "B" + fieldText});

        Assert.assertEquals("BAff", f.format("f"));
    }

    @Test
    public void testDoubleComposite() {

        LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(), new NoSpaceBetweenAbbreviations());
        LayoutFormatter first = new AuthorOrgSci();
        LayoutFormatter second = new NoSpaceBetweenAbbreviations();

        Assert.assertEquals(second.format(first.format("John Flynn and Sabine Gartska")),
                f.format("John Flynn and Sabine Gartska"));
        Assert.assertEquals(second.format(first.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee")),
                f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
    }

}
