package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AuthorOrgSciTest {

    @Test
    public void testOrgSci() {
        LayoutFormatter f = new AuthorOrgSci();

        assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
        assertEquals("Garvin, D. A.", f.format("David A. Garvin"));
        assertEquals("Makridakis, S., S. C. Wheelwright, V. E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));

    }

    @Test
    public void testOrgSciPlusAbbreviation() {
        LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(), new NoSpaceBetweenAbbreviations());
        assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
        assertEquals("Garvin, D.A.", f.format("David A. Garvin"));
        assertEquals("Makridakis, S., S.C. Wheelwright, V.E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
    }
}
