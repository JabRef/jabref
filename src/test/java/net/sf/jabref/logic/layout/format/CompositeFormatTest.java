package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class CompositeFormatTest {

    @Test
    public void testComposite() {

        {
            LayoutFormatter f = new CompositeFormat();
            Assert.assertEquals("No Change", f.format("No Change"));
        }
        {
            LayoutFormatter f = new CompositeFormat(new LayoutFormatter[] {new LayoutFormatter() {

                @Override
                public String format(String fieldText) {
                    return fieldText + fieldText;
                }

            }, new LayoutFormatter() {

                @Override
                public String format(String fieldText) {
                    return "A" + fieldText;
                }

            }, new LayoutFormatter() {

                @Override
                public String format(String fieldText) {
                    return "B" + fieldText;
                }

            }});

            Assert.assertEquals("BAff", f.format("f"));
        }

        LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(),
                new NoSpaceBetweenAbbreviations());
        LayoutFormatter first = new AuthorOrgSci();
        LayoutFormatter second = new NoSpaceBetweenAbbreviations();

        Assert.assertEquals(second.format(first.format("John Flynn and Sabine Gartska")), f.format("John Flynn and Sabine Gartska"));
        Assert.assertEquals(second.format(first.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee")), f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
    }

}
