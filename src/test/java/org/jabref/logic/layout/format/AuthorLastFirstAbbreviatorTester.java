package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test case  that verifies the functionalities of the formater AuthorLastFirstAbbreviator.
 */
class AuthorLastFirstAbbreviatorTester {

    /**
     * Verifies the Abbreviation of one single author with a simple name.
     * <p/>
     * Ex: Lastname, Name
     */
    @Test
    void testOneAuthorSimpleName() {
        assertEquals("Lastname, N.", abbreviate("Lastname, Name"));
    }

    /**
     * Verifies the Abbreviation of one single author with a common name.
     * <p/>
     * Ex: Lastname, Name Middlename
     */
    @Test
    void testOneAuthorCommonName() {
        assertEquals("Lastname, N. M.", abbreviate("Lastname, Name Middlename"));
    }

    /**
     * Verifies the Abbreviation of two single with a common name.
     * <p/>
     * Ex: Lastname, Name Middlename
     */
    @Test
    void testTwoAuthorsCommonName() {
        String result = abbreviate("Lastname, Name Middlename and Sobrenome, Nome Nomedomeio");
        String expectedResult = "Lastname, N. M. and Sobrenome, N. N.";

        assertEquals(expectedResult, result);
    }

    @Test
    void testJrAuthor() {
        assertEquals("Other, Jr., A. N.", abbreviate("Other, Jr., Anthony N."));
    }

    @Test
    void testFormat() {
        assertEquals("", abbreviate(""));
        assertEquals("Someone, V. S.", abbreviate("Someone, Van Something"));
        assertEquals("Smith, J.", abbreviate("Smith, John"));
        assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.",
                abbreviate("von Neumann, John and Smith, John and Black Brown, Peter"));
    }

    private String abbreviate(String name) {
        return new AuthorLastFirstAbbreviator().format(name);
    }
}
