package org.jabref.logic.layout.format;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Test case  that verifies the functionalities of the
 * formater AuthorLastFirstAbbreviator.
 *
 * @author Carlos Silla
 * @author Christopher Oezbek <oezi@oezi.de>
 */
public class AuthorLastFirstAbbreviatorTester {

    /**
     * Verifies the Abbreviation of one single author with a simple name.
     * <p/>
     * Ex: Lastname, Name
     */
    @Test
    public void testOneAuthorSimpleName() {
        assertEquals("Abbreviator Test", "Lastname, N.", abbreviate("Lastname, Name"));
    }

    /**
     * Verifies the Abbreviation of one single author with a common name.
     * <p/>
     * Ex: Lastname, Name Middlename
     */
    @Test
    public void testOneAuthorCommonName() {
        assertEquals("Abbreviator Test", "Lastname, N. M.", abbreviate("Lastname, Name Middlename"));
    }

    /**
     * Verifies the Abbreviation of two single with a common name.
     * <p/>
     * Ex: Lastname, Name Middlename
     */
    @Test
    public void testTwoAuthorsCommonName() {
        String result = abbreviate("Lastname, Name Middlename and Sobrenome, Nome Nomedomeio");
        String expectedResult = "Lastname, N. M. and Sobrenome, N. N.";

        assertEquals("Abbreviator Test", expectedResult, result);
    }

    @Test
    public void testJrAuthor() {
        assertEquals("Other, Jr., A. N.", abbreviate("Other, Jr., Anthony N."));
    }

    @Test
    public void testFormat() {
        assertEquals("", abbreviate(""));
        assertEquals("Someone, V. S.", abbreviate("Someone, Van Something"));
        assertEquals("Smith, J.", abbreviate("Smith, John"));
        assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.",
                abbreviate("von Neumann, John and Smith, John and Black Brown, Peter"));
    }

    protected String abbreviate(String name) {
        return new AuthorLastFirstAbbreviator().format(name);
    }

}
