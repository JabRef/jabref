package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorOrgSciTest {

    @Test
    public void testOrgSci() {
        LayoutFormatter f = new AuthorOrgSci();

        Assert.assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
        Assert.assertEquals("Garvin, D. A.", f.format("David A. Garvin"));
        Assert.assertEquals("Makridakis, S., S. C. Wheelwright, V. E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));

    }

    @Test
    public void testOrgSciPlusAbbreviation() {
        LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(), new NoSpaceBetweenAbbreviations());
        Assert.assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
        Assert.assertEquals("Garvin, D.A.", f.format("David A. Garvin"));
        Assert.assertEquals("Makridakis, S., S.C. Wheelwright, V.E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
    }
}
