package org.jabref.model.entry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorTest {

    @Test
    void addDotIfAbbreviationAddDot() {
        assertEquals("O.", Author.addDotIfAbbreviation("O"));
        assertEquals("A. O.", Author.addDotIfAbbreviation("AO"));
        assertEquals("A. O.", Author.addDotIfAbbreviation("AO."));
        assertEquals("A. O.", Author.addDotIfAbbreviation("A.O."));
        assertEquals("A.-O.", Author.addDotIfAbbreviation("A-O"));
    }

    @Test
    void addDotIfAbbreviationDoesNotAddMultipleSpaces() {
        assertEquals("A. O.", Author.addDotIfAbbreviation("A O"));
    }

    @Test
    void addDotIfAbbreviationDoNotAddDot() {
        assertEquals("O.", Author.addDotIfAbbreviation("O."));
        assertEquals("A. O.", Author.addDotIfAbbreviation("A. O."));
        assertEquals("A.-O.", Author.addDotIfAbbreviation("A.-O."));
        assertEquals("O. Moore", Author.addDotIfAbbreviation("O. Moore"));
        assertEquals("A. O. Moore", Author.addDotIfAbbreviation("A. O. Moore"));
        assertEquals("O. von Moore", Author.addDotIfAbbreviation("O. von Moore"));
        assertEquals("A.-O. Moore", Author.addDotIfAbbreviation("A.-O. Moore"));
        assertEquals("Moore, O.", Author.addDotIfAbbreviation("Moore, O."));
        assertEquals("Moore, O., Jr.", Author.addDotIfAbbreviation("Moore, O., Jr."));
        assertEquals("Moore, A. O.", Author.addDotIfAbbreviation("Moore, A. O."));
        assertEquals("Moore, A.-O.", Author.addDotIfAbbreviation("Moore, A.-O."));
        assertEquals("MEmre", Author.addDotIfAbbreviation("MEmre"));
        assertEquals("{\\'{E}}douard", Author.addDotIfAbbreviation("{\\'{E}}douard"));
        assertEquals("J{\\\"o}rg", Author.addDotIfAbbreviation("J{\\\"o}rg"));
        assertEquals("Moore, O. and O. Moore", Author.addDotIfAbbreviation("Moore, O. and O. Moore"));
        assertEquals("Moore, O. and O. Moore and Moore, O. O.", Author.addDotIfAbbreviation("Moore, O. and O. Moore and Moore, O. O."));
    }
    
    @Test
    public void testAuthEtAlBraces() {
        assertEquals("{\\v{S}}pan{\\v{e}}l",
                     BibtexKeyGenerator.authEtal("Patrik {\\v{S}}pan{\\v{e}}l and Kseniya Dryahina and David Smith", "", "EtAl"));
        assertEquals("\\v{S}pan\\v{e}lEtAl",
                     BibtexKeyGenerator.authEtal("Patrik \\v{S}pan\\v{e}l and Kseniya Dryahina and David Smith", "", "EtAl"));
    }
}
