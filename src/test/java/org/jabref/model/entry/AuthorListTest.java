package org.jabref.model.entry;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthorListTest {

    /*
    Examples are similar to page 4 in
    [BibTeXing by Oren Patashnik](https://ctan.org/tex-archive/biblio/bibtex/contrib/doc/)
    */
    private static final String MUHAMMAD_ALKHWARIZMI = "Mu{\\d{h}}ammad al-Khw{\\={a}}rizm{\\={i}}";
    private static final String CORRADO_BOHM = "Corrado B{\\\"o}hm";
    private static final String KURT_GODEL = "Kurt G{\\\"{o}}del";
    private static final String BANU_MOSA = "{The Ban\\={u} M\\={u}s\\={a} brothers}";

    public static int size(String bibtex) {
        return AuthorList.parse(bibtex).getNumberOfAuthors();
    }

    @Test
    public void testFixAuthorNatbib() {
        assertEquals("", AuthorList.fixAuthorNatbib(""));
        assertEquals("Smith", AuthorList.fixAuthorNatbib("John Smith"));
        assertEquals("Smith and Black Brown", AuthorList
                .fixAuthorNatbib("John Smith and Black Brown, Peter"));
        assertEquals("von Neumann et al.", AuthorList
                .fixAuthorNatbib("John von Neumann and John Smith and Black Brown, Peter"));

        // Is not cached!
        assertTrue(AuthorList
                .fixAuthorNatbib("John von Neumann and John Smith and Black Brown, Peter").equals(AuthorList
                        .fixAuthorNatbib("John von Neumann and John Smith and Black Brown, Peter")));
    }

    @Test
    public void fixAuthorNatbibLatexFreeNullAuthorStringThrowsException() {
        assertThrows(NullPointerException.class, () -> AuthorList.fixAuthorNatbibLatexFree(null));
    }

    @Test
    public void fixAuthorNatbibLatexFreeEmptyAuthorStringForEmptyInput() {
        assertEquals("", AuthorList.fixAuthorNatbibLatexFree(""));
    }

    @Test
    public void fixAuthorNatbibLatexFreeCachesLatexFreeString() {
        String cachedString = AuthorList
                .fixAuthorNatbibLatexFree(MUHAMMAD_ALKHWARIZMI);
        assertSame(cachedString, AuthorList
                .fixAuthorNatbibLatexFree(MUHAMMAD_ALKHWARIZMI));
    }

    @Test
    public void fixAuthorNatbibLatexFreeUnicodeOneAuthorNameFromLatex() {
        assertEquals("al-Khwārizmī",
                AuthorList.fixAuthorNatbibLatexFree(MUHAMMAD_ALKHWARIZMI));
    }

    @Test
    public void fixAuthorNatbibLatexFreeUnicodeTwoAuthorNamesFromLatex() {
        assertEquals("al-Khwārizmī and Böhm",
                AuthorList.fixAuthorNatbibLatexFree(MUHAMMAD_ALKHWARIZMI
                + " and " + CORRADO_BOHM));
    }

    @Test
    public void fixAuthorNatbibLatexFreeUnicodeAuthorEtAlFromLatex() {
        assertEquals("al-Khwārizmī et al.",
                AuthorList.fixAuthorNatbibLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL));
    }

    @Test
    public void fixAuthorNatbibLatexFreeUnicodeOneInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorNatbibLatexFree(BANU_MOSA));
    }

    @Test
    public void fixAuthorNatbibLatexFreeUnicodeTwoInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorNatbibLatexFree(BANU_MOSA
                + " and " + BANU_MOSA));
    }

    @Test
    public void fixAuthorNatbibLatexFreeUnicodeMixedAuthorsFromLatex() {
        assertEquals("The Banū Mūsā brothers and Böhm",
                AuthorList.fixAuthorNatbibLatexFree(BANU_MOSA
                + " and " + CORRADO_BOHM));
    }

    @Test
    public void fixAuthorNatbibLatexFreeOneInstitutionWithParanthesisAtStart() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorNatbibLatexFree("{{\\L{}}ukasz Micha\\l{}}"));
    }

    @Test
    public void fixAuthorNatbibLatexFreeAuthorWithEscapedBrackets() {
        assertEquals("Mic}h{ał",
                AuthorList.fixAuthorNatbibLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}"));
    }

    @Test
    public void fixAuthorNatbibLatexFreeInstituteAuthorWithEscapedBrackets() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorNatbibLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}"));
    }

    @Test
    public void testGetAuthorList() {
        // Test caching in authorCache.
        AuthorList al = AuthorList.parse("John Smith");
        assertEquals(al, AuthorList.parse("John Smith"));
        assertFalse(al.equals(AuthorList.parse("Smith")));
    }

    @Test
    public void testFixAuthorFirstNameFirstCommas() {

        // No Commas
        assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", true, false));
        assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", false, false));

        assertEquals("John Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith",
                false, false));
        assertEquals("J. Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith", true,
                false));

        // Check caching
        assertTrue(AuthorList.fixAuthorFirstNameFirstCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true, false).equals(
                AuthorList
                        .fixAuthorFirstNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter", true, false)));

        assertEquals("John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas("John Smith and Black Brown, Peter", false, false));
        assertEquals("J. Smith and P. Black Brown", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Smith and Black Brown, Peter", true, false));

        // Method description is different than code -> additional comma
        // there
        assertEquals("John von Neumann, John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false, false));
        assertEquals("J. von Neumann, J. Smith and P. Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true, false));

        assertEquals("J. P. von Neumann", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Peter von Neumann", true, false));
        // Oxford Commas
        assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", true, true));
        assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", false, true));

        assertEquals("John Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith",
                false, true));
        assertEquals("J. Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith", true,
                true));

        // Check caching
        assertTrue(AuthorList.fixAuthorFirstNameFirstCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true, true).equals(
                AuthorList
                        .fixAuthorFirstNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter", true, true)));

        assertEquals("John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas("John Smith and Black Brown, Peter", false, true));
        assertEquals("J. Smith and P. Black Brown", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Smith and Black Brown, Peter", true, true));

        // Method description is different than code -> additional comma
        // there
        assertEquals("John von Neumann, John Smith, and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false, true));
        assertEquals("J. von Neumann, J. Smith, and P. Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true, true));

        assertEquals("J. P. von Neumann", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Peter von Neumann", true, true));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeNullAuthorStringThrowsExceptionAbbr() {
        assertThrows(NullPointerException.class,
                () -> AuthorList.fixAuthorFirstNameFirstCommasLatexFree(null, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeEmptyAuthorStringForEmptyInputAbbr() {
        assertEquals("", AuthorList.fixAuthorFirstNameFirstCommasLatexFree("", true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeCachesLatexFreeStringAbbr() {
        String cachedString = AuthorList
                .fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, false);
        assertSame(cachedString, AuthorList
                .fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeOneAuthorNameFromLatexAbbr() {
        assertEquals("M. al-Khwārizmī",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeTwoAuthorNamesFromLatexAbbr() {
        assertEquals("M. al-Khwārizmī and C. Böhm",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeThreeUnicodeAuthorsFromLatexAbbr() {
        assertEquals("M. al-Khwārizmī, C. Böhm and K. Gödel",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeOneInsitutionNameFromLatexAbbr() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(BANU_MOSA, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeTwoInsitutionNameFromLatexAbbr() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + BANU_MOSA, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeMixedAuthorsFromLatexAbbr() {
        assertEquals("The Banū Mūsā brothers and C. Böhm",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + CORRADO_BOHM, true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeOneInstitutionWithParanthesisAtStartAbbr() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree("{{\\L{}}ukasz Micha\\l{}}",
                        true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeAuthorWithEscapedBracketsAbbr() {
        assertEquals("Ł. Mic}h{ał",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}",
                        true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeInstituteAuthorWithEscapedBracketsAbbr() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}",
                        true, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeNullAuthorStringThrowsException() {
        assertThrows(NullPointerException.class,
                () -> AuthorList.fixAuthorFirstNameFirstCommasLatexFree(null, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeEmptyAuthorStringForEmptyInput() {
        assertEquals("", AuthorList.fixAuthorFirstNameFirstCommasLatexFree("", false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeCachesLatexFreeString() {
        String cachedString = AuthorList
                .fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, false);
        assertSame(cachedString, AuthorList
                .fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeOneAuthorNameFromLatex() {
        assertEquals("Muḥammad al-Khwārizmī",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeTwoAuthorNamesFromLatex() {
        assertEquals("Muḥammad al-Khwārizmī and Corrado Böhm",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeThreeUnicodeAuthorsFromLatex() {
        assertEquals("Muḥammad al-Khwārizmī, Corrado Böhm and Kurt Gödel",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeOneInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(BANU_MOSA, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeTwoInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + BANU_MOSA, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeUnicodeMixedAuthorsFromLatex() {
        assertEquals("The Banū Mūsā brothers and Corrado Böhm",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + CORRADO_BOHM, false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeOneInstitutionWithParanthesisAtStart() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree("{{\\L{}}ukasz Micha\\l{}}",
                        false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeAuthorWithEscapedBrackets() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}",
                        false, false));
    }

    @Test
    public void fixAuthorFirstNameFirstCommasLatexFreeInstituteAuthorWithEscapedBrackets() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorFirstNameFirstCommasLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}",
                        false, false));
    }

    @Test
    public void testFixAuthorFirstNameFirst() {
        assertEquals("John Smith", AuthorList.fixAuthorFirstNameFirst("John Smith"));

        assertEquals("John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirst("John Smith and Black Brown, Peter"));

        assertEquals("John von Neumann and John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirst("John von Neumann and John Smith and Black Brown, Peter"));

        assertEquals("First von Last, Jr. III", AuthorList
                .fixAuthorFirstNameFirst("von Last, Jr. III, First"));

        // Check caching
        assertTrue(AuthorList
                .fixAuthorFirstNameFirst("John von Neumann and John Smith and Black Brown, Peter").equals(AuthorList
                        .fixAuthorFirstNameFirst("John von Neumann and John Smith and Black Brown, Peter")));
    }

    @Test
    public void testFixAuthorLastNameFirstCommasNoComma() {
        // No commas before and
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", true, false));
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", false, false));

        assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirstCommas("John Smith", false, false));
        assertEquals("Smith, J.", AuthorList.fixAuthorLastNameFirstCommas("John Smith", true, false));

        String a = AuthorList.fixAuthorLastNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter",
                true, false);
        String b = AuthorList.fixAuthorLastNameFirstCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true, false);

        // Check caching
        assertEquals(a, b);
        assertTrue(a.equals(b));

        assertEquals("Smith, John and Black Brown, Peter",
                AuthorList.fixAuthorLastNameFirstCommas("John Smith and Black Brown, Peter", false, false));
        assertEquals("Smith, J. and Black Brown, P.",
                AuthorList.fixAuthorLastNameFirstCommas("John Smith and Black Brown, Peter", true, false));

        assertEquals("von Neumann, John, Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter", false, false));
        assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", AuthorList
                .fixAuthorLastNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter", true, false));

        assertEquals("von Neumann, J. P.",
                AuthorList.fixAuthorLastNameFirstCommas("John Peter von Neumann", true, false));
    }

    @Test
    public void testFixAuthorLastNameFirstCommasOxfordComma() {
        // Oxford Commas
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", true, true));
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", false, true));

        assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirstCommas("John Smith",
                false, true));
        assertEquals("Smith, J.", AuthorList.fixAuthorLastNameFirstCommas("John Smith", true,
                true));

        String a = AuthorList.fixAuthorLastNameFirstCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true, true);
        String b = AuthorList.fixAuthorLastNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter", true, true);

        // Check caching
        assertEquals(a, b);
        assertTrue(a.equals(b));

        assertEquals("Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirstCommas("John Smith and Black Brown, Peter", false, true));
        assertEquals("Smith, J. and Black Brown, P.", AuthorList.fixAuthorLastNameFirstCommas(
                "John Smith and Black Brown, Peter", true, true));

        assertEquals("von Neumann, John, Smith, John, and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false, true));
        assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", AuthorList
                .fixAuthorLastNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true, true));

        assertEquals("von Neumann, J. P.", AuthorList.fixAuthorLastNameFirstCommas(
                "John Peter von Neumann", true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeNullAuthorStringThrowsExceptionAbbr() {
        assertThrows(NullPointerException.class,
                () -> AuthorList.fixAuthorLastNameFirstCommasLatexFree(null, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeEmptyAuthorStringForEmptyInputAbbr() {
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommasLatexFree("", true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeCachesLatexFreeStringAbbr() {
        String cachedString = AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, false);
        assertSame(cachedString, AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneAuthorNameFromLatexAbbr() {
        assertEquals("al-Khwārizmī, M.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoAuthorNamesFromLatexAbbr() {
        assertEquals("al-Khwārizmī, M. and Böhm, C.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeThreeUnicodeAuthorsFromLatexAbbr() {
        assertEquals("al-Khwārizmī, M., Böhm, C. and Gödel, K.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneInsitutionNameFromLatexAbbr() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoInsitutionNameFromLatexAbbr() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + BANU_MOSA, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeMixedAuthorsFromLatexAbbr() {
        assertEquals("The Banū Mūsā brothers and Böhm, C.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + CORRADO_BOHM, true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeOneInstitutionWithParanthesisAtStartAbbr() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Micha\\l{}}",
                        true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeAuthorWithEscapedBracketsAbbr() {
        assertEquals("Mic}h{ał, Ł.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}",
                        true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeInstituteAuthorWithEscapedBracketsAbbr() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}",
                        true, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeNullAuthorStringThrowsException() {
        assertThrows(NullPointerException.class,
                () -> AuthorList.fixAuthorLastNameFirstCommasLatexFree(null, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeEmptyAuthorStringForEmptyInput() {
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommasLatexFree("", false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeCachesLatexFreeString() {
        String cachedString = AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, false);
        assertSame(cachedString, AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneAuthorNameFromLatex() {
        assertEquals("al-Khwārizmī, Muḥammad",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoAuthorNamesFromLatex() {
        assertEquals("al-Khwārizmī, Muḥammad and Böhm, Corrado",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeThreeUnicodeAuthorsFromLatex() {
        assertEquals("al-Khwārizmī, Muḥammad, Böhm, Corrado and Gödel, Kurt",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + BANU_MOSA, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeMixedAuthorsFromLatex() {
        assertEquals("The Banū Mūsā brothers and Böhm, Corrado",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + CORRADO_BOHM, false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeOneInstitutionWithParanthesisAtStart() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Micha\\l{}}",
                        false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeAuthorWithEscapedBrackets() {
        assertEquals("Mic}h{ał, Łukasz",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}",
                        false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeInstituteAuthorWithEscapedBrackets() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}",
                        false, false));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeNullAuthorStringThrowsExceptionAbbrOxfordComma() {
        assertThrows(NullPointerException.class,
                () -> AuthorList.fixAuthorLastNameFirstCommasLatexFree(null, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeEmptyAuthorStringForEmptyInputAbbrOxfordComma() {
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommasLatexFree("", true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeCachesLatexFreeStringAbbrOxfordComma() {
        String cachedString = AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, true);
        assertSame(cachedString, AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneAuthorNameFromLatexAbbrOxfordComma() {
        assertEquals("al-Khwārizmī, M.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoAuthorNamesFromLatexAbbrOxfordComma() {
        assertEquals("al-Khwārizmī, M. and Böhm, C.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeThreeUnicodeAuthorsFromLatexAbbrOxfordComma() {
        assertEquals("al-Khwārizmī, M., Böhm, C., and Gödel, K.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneInsitutionNameFromLatexAbbrOxfordComma() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoInsitutionNameFromLatexAbbrOxfordComma() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + BANU_MOSA, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeMixedAuthorsFromLatexAbbrOxfordComma() {
        assertEquals("The Banū Mūsā brothers and Böhm, C.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + CORRADO_BOHM, true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeOneInstitutionWithParanthesisAtStartAbbrOxfordComma() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Micha\\l{}}",
                        true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeAuthorWithEscapedBracketsAbbrOxfordComma() {
        assertEquals("Mic}h{ał, Ł.",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}",
                        true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeInstituteAuthorWithEscapedBracketsAbbrOxfordComma() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}",
                        true, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeNullAuthorStringThrowsExceptionOxfordComma() {
        assertThrows(NullPointerException.class,
                () -> AuthorList.fixAuthorLastNameFirstCommasLatexFree(null, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeEmptyAuthorStringForEmptyInputOxfordComma() {
        assertEquals("", AuthorList.fixAuthorLastNameFirstCommasLatexFree("", false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeCachesLatexFreeStringOxfordComma() {
        String cachedString = AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, true);
        assertSame(cachedString, AuthorList
                .fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneAuthorNameFromLatexOxfordComma() {
        assertEquals("al-Khwārizmī, Muḥammad",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoAuthorNamesFromLatexOxfordComma() {
        assertEquals("al-Khwārizmī, Muḥammad and Böhm, Corrado",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeThreeUnicodeAuthorsFromLatexOxfordComma() {
        assertEquals("al-Khwārizmī, Muḥammad, Böhm, Corrado, and Gödel, Kurt",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeOneInsitutionNameFromLatexOxfordComma() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeTwoInsitutionNameFromLatexOxfordComma() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + BANU_MOSA, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeUnicodeMixedAuthorsFromLatexOxfordComma() {
        assertEquals("The Banū Mūsā brothers and Böhm, Corrado",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree(BANU_MOSA
                        + " and " + CORRADO_BOHM, false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeOneInstitutionWithParanthesisAtStartOxfordComma() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Micha\\l{}}",
                        false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeAuthorWithEscapedBracketsOxfordComma() {
        assertEquals("Mic}h{ał, Łukasz",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}",
                        false, true));
    }

    @Test
    public void fixAuthorLastNameFirstCommasLatexFreeInstituteAuthorWithEscapedBracketsOxfordComma() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorLastNameFirstCommasLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}",
                        false, true));
    }

    @Test
    public void testFixAuthorLastNameFirst() {

        // Test helper method

        assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirst("John Smith"));

        assertEquals("Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirst("John Smith and Black Brown, Peter"));

        assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter"));

        assertEquals("von Last, Jr, First", AuthorList
                .fixAuthorLastNameFirst("von Last, Jr ,First"));

        assertTrue(AuthorList
                .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter").equals(AuthorList
                        .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter")));

        // Test Abbreviation == false
        assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirst("John Smith", false));

        assertEquals("Smith, John and Black Brown, Peter", AuthorList.fixAuthorLastNameFirst(
                "John Smith and Black Brown, Peter", false));

        assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter",
                        false));

        assertEquals("von Last, Jr, First", AuthorList.fixAuthorLastNameFirst(
                "von Last, Jr ,First", false));

        assertTrue(AuthorList.fixAuthorLastNameFirst(
                "John von Neumann and John Smith and Black Brown, Peter", false).equals(
                AuthorList
                        .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter", false)));

        // Test Abbreviate == true
        assertEquals("Smith, J.", AuthorList.fixAuthorLastNameFirst("John Smith", true));

        assertEquals("Smith, J. and Black Brown, P.", AuthorList.fixAuthorLastNameFirst(
                "John Smith and Black Brown, Peter", true));

        assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.",
                AuthorList.fixAuthorLastNameFirst(
                        "John von Neumann and John Smith and Black Brown, Peter", true));

        assertEquals("von Last, Jr, F.", AuthorList.fixAuthorLastNameFirst("von Last, Jr ,First",
                true));

        assertTrue(AuthorList.fixAuthorLastNameFirst(
                "John von Neumann and John Smith and Black Brown, Peter", true).equals(
                AuthorList
                        .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter", true)));
    }

    @Test
    public void testFixAuthorLastNameOnlyCommas() {

        // No comma before and
        assertEquals("", AuthorList.fixAuthorLastNameOnlyCommas("", false));
        assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("John Smith", false));
        assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("Smith, Jr, John", false));

        assertTrue(AuthorList.fixAuthorLastNameOnlyCommas(
                "John von Neumann and John Smith and Black Brown, Peter", false).equals(
                AuthorList
                        .fixAuthorLastNameOnlyCommas("John von Neumann and John Smith and Black Brown, Peter", false)));

        assertEquals("von Neumann, Smith and Black Brown", AuthorList
                .fixAuthorLastNameOnlyCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false));
        // Oxford Comma
        assertEquals("", AuthorList.fixAuthorLastNameOnlyCommas("", true));
        assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("John Smith", true));
        assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("Smith, Jr, John", true));

        assertTrue(AuthorList.fixAuthorLastNameOnlyCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true).equals(
                AuthorList
                        .fixAuthorLastNameOnlyCommas("John von Neumann and John Smith and Black Brown, Peter", true)));

        assertEquals("von Neumann, Smith, and Black Brown", AuthorList
                .fixAuthorLastNameOnlyCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeNullAuthorStringThrowsException() {
        assertThrows(NullPointerException.class,
                () -> AuthorList.fixAuthorLastNameOnlyCommasLatexFree(null, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeEmptyAuthorStringForEmptyInput() {
        assertEquals("", AuthorList.fixAuthorLastNameOnlyCommasLatexFree("", false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeCachesLatexFreeString() {
        String cachedString = AuthorList
                .fixAuthorLastNameOnlyCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false);
        assertSame(cachedString, AuthorList
                .fixAuthorLastNameOnlyCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeUnicodeOneAuthorNameFromLatex() {
        assertEquals("al-Khwārizmī",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree(MUHAMMAD_ALKHWARIZMI, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeUnicodeTwoAuthorNamesFromLatex() {
        assertEquals("al-Khwārizmī and Böhm",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeUnicodeThreeUnicodeAuthorsFromLatex() {
        assertEquals("al-Khwārizmī, Böhm and Gödel",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree(MUHAMMAD_ALKHWARIZMI
                        + " and " + CORRADO_BOHM + " and " + KURT_GODEL, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeUnicodeOneInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree(BANU_MOSA, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeUnicodeTwoInsitutionNameFromLatex() {
        assertEquals("The Banū Mūsā brothers and The Banū Mūsā brothers",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree(BANU_MOSA
                        + " and " + BANU_MOSA, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeUnicodeMixedAuthorsFromLatex() {
        assertEquals("The Banū Mūsā brothers and Böhm",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree(BANU_MOSA
                        + " and " + CORRADO_BOHM, false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeOneInstitutionWithParanthesisAtStart() {
        assertEquals("Łukasz Michał",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree("{{\\L{}}ukasz Micha\\l{}}", false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeAuthorWithEscapedBrackets() {
        assertEquals("Mic}h{ał",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree("{\\L{}}ukasz Mic\\}h\\{a\\l{}", false));
    }

    @Test
    public void fixAuthorLastNameOnlyCommasLatexFreeInstituteAuthorWithEscapedBrackets() {
        assertEquals("Łukasz Mic}h{ał",
                AuthorList.fixAuthorLastNameOnlyCommasLatexFree("{{\\L{}}ukasz Mic\\}h\\{a\\l{}}", false));
    }

    @Test
    public void testFixAuthorForAlphabetization() {
        assertEquals("Smith, J.", AuthorList.fixAuthorForAlphabetization("John Smith"));
        assertEquals("Neumann, J.", AuthorList.fixAuthorForAlphabetization("John von Neumann"));
        assertEquals("Neumann, J.", AuthorList.fixAuthorForAlphabetization("J. von Neumann"));
        assertEquals(
                "Neumann, J. and Smith, J. and Black Brown, Jr., P.",
                AuthorList
                        .fixAuthorForAlphabetization("John von Neumann and John Smith and de Black Brown, Jr., Peter"));
    }

    @Test
    public void testSize() {

        assertEquals(0, AuthorListTest.size(""));
        assertEquals(1, AuthorListTest.size("Bar"));
        assertEquals(1, AuthorListTest.size("Foo Bar"));
        assertEquals(1, AuthorListTest.size("Foo von Bar"));
        assertEquals(1, AuthorListTest.size("von Bar, Foo"));
        assertEquals(1, AuthorListTest.size("Bar, Foo"));
        assertEquals(1, AuthorListTest.size("Bar, Jr., Foo"));
        assertEquals(1, AuthorListTest.size("Bar, Foo"));
        assertEquals(2, AuthorListTest.size("John Neumann and Foo Bar"));
        assertEquals(2, AuthorListTest.size("John von Neumann and Bar, Jr, Foo"));

        assertEquals(3, AuthorListTest.size("John von Neumann and John Smith and Black Brown, Peter"));

        StringBuilder s = new StringBuilder("John von Neumann");
        for (int i = 0; i < 25; i++) {
            assertEquals(i + 1, AuthorListTest.size(s.toString()));
            s.append(" and Albert Einstein");
        }
    }

    @Test
    public void testIsEmpty() {
        assertTrue(AuthorList.parse("").isEmpty());
        assertFalse(AuthorList.parse("Bar").isEmpty());
    }

    @Test
    public void testGetEmptyAuthor() {
        assertThrows(Exception.class, () -> AuthorList.parse("").getAuthor(0));
    }

    @Test
    public void testGetAuthor() {

        Author author = AuthorList.parse("John Smith and von Neumann, Jr, John").getAuthor(0);
        assertEquals(Optional.of("John"), author.getFirst());
        assertEquals(Optional.of("J."), author.getFirstAbbr());
        assertEquals("John Smith", author.getFirstLast(false));
        assertEquals("J. Smith", author.getFirstLast(true));
        assertEquals(Optional.empty(), author.getJr());
        assertEquals(Optional.of("Smith"), author.getLast());
        assertEquals("Smith, John", author.getLastFirst(false));
        assertEquals("Smith, J.", author.getLastFirst(true));
        assertEquals("Smith", author.getLastOnly());
        assertEquals("Smith, J.", author.getNameForAlphabetization());
        assertEquals(Optional.empty(), author.getVon());

        author = AuthorList.parse("Peter Black Brown").getAuthor(0);
        assertEquals(Optional.of("Peter Black"), author.getFirst());
        assertEquals(Optional.of("P. B."), author.getFirstAbbr());
        assertEquals("Peter Black Brown", author.getFirstLast(false));
        assertEquals("P. B. Brown", author.getFirstLast(true));
        assertEquals(Optional.empty(), author.getJr());
        assertEquals(Optional.empty(), author.getVon());

        author = AuthorList.parse("John Smith and von Neumann, Jr, John").getAuthor(1);
        assertEquals(Optional.of("John"), author.getFirst());
        assertEquals(Optional.of("J."), author.getFirstAbbr());
        assertEquals("John von Neumann, Jr", author.getFirstLast(false));
        assertEquals("J. von Neumann, Jr", author.getFirstLast(true));
        assertEquals(Optional.of("Jr"), author.getJr());
        assertEquals(Optional.of("Neumann"), author.getLast());
        assertEquals("von Neumann, Jr, John", author.getLastFirst(false));
        assertEquals("von Neumann, Jr, J.", author.getLastFirst(true));
        assertEquals("von Neumann", author.getLastOnly());
        assertEquals("Neumann, Jr, J.", author.getNameForAlphabetization());
        assertEquals(Optional.of("von"), author.getVon());
    }

    @Test
    public void testCompanyAuthor() {
        Author author = AuthorList.parse("{JabRef Developers}").getAuthor(0);
        Author expected = new Author(null, null, null, "JabRef Developers", null);
        assertEquals(expected, author);
    }

    @Test
    public void testCompanyAuthorAndPerson() {
        Author company = new Author(null, null, null, "JabRef Developers", null);
        Author person = new Author("Stefan", "S.", null, "Kolb", null);
        assertEquals(Arrays.asList(company, person), AuthorList.parse("{JabRef Developers} and Stefan Kolb").getAuthors());
    }

    @Test
    public void testCompanyAuthorWithLowerCaseWord() {
        Author author = AuthorList.parse("{JabRef Developers on Fire}").getAuthor(0);
        Author expected = new Author(null, null, null, "JabRef Developers on Fire", null);
        assertEquals(expected, author);
    }

    @Test
    public void testAbbreviationWithRelax() {
        Author author = AuthorList.parse("{\\relax Ch}ristoph Cholera").getAuthor(0);
        Author expected = new Author("{\\relax Ch}ristoph", "{\\relax Ch}.", null, "Cholera", null);
        assertEquals(expected, author);
    }

    @Test
    public void testGetAuthorsNatbib() {
        assertEquals("", AuthorList.parse("").getAsNatbib());
        assertEquals("Smith", AuthorList.parse("John Smith").getAsNatbib());
        assertEquals("Smith and Black Brown", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsNatbib());
        assertEquals("von Neumann et al.", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsNatbib());

        /*
         * [ 1465610 ] (Double-)Names containing hyphen (-) not handled correctly
         */
        assertEquals("Last-Name et al.", AuthorList.parse(
                "First Second Last-Name" + " and John Smith and Black Brown, Peter").getAsNatbib());

        // Test caching
        AuthorList al = AuthorList
                .parse("John von Neumann and John Smith and Black Brown, Peter");
        assertTrue(al.getAsNatbib().equals(al.getAsNatbib()));
    }

    @Test
    public void testGetAuthorsLastOnly() {

        // No comma before and
        assertEquals("", AuthorList.parse("").getAsLastNames(false));
        assertEquals("Smith", AuthorList.parse("John Smith").getAsLastNames(false));
        assertEquals("Smith", AuthorList.parse("Smith, Jr, John").getAsLastNames(
                false));

        assertEquals("von Neumann, Smith and Black Brown", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsLastNames(false));
        // Oxford comma
        assertEquals("", AuthorList.parse("").getAsLastNames(true));
        assertEquals("Smith", AuthorList.parse("John Smith").getAsLastNames(true));
        assertEquals("Smith", AuthorList.parse("Smith, Jr, John").getAsLastNames(
                true));

        assertEquals("von Neumann, Smith, and Black Brown", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsLastNames(true));

        assertEquals("von Neumann and Smith",
                AuthorList.parse("John von Neumann and John Smith").getAsLastNames(false));
    }

    @Test
    public void testGetAuthorsLastFirstNoComma() {
        // No commas before and
        AuthorList al;

        al = AuthorList.parse("");
        assertEquals("", al.getAsLastFirstNames(true, false));
        assertEquals("", al.getAsLastFirstNames(false, false));

        al = AuthorList.parse("John Smith");
        assertEquals("Smith, John", al.getAsLastFirstNames(false, false));
        assertEquals("Smith, J.", al.getAsLastFirstNames(true, false));

        al = AuthorList.parse("John Smith and Black Brown, Peter");
        assertEquals("Smith, John and Black Brown, Peter", al.getAsLastFirstNames(false, false));
        assertEquals("Smith, J. and Black Brown, P.", al.getAsLastFirstNames(true, false));

        al = AuthorList.parse("John von Neumann and John Smith and Black Brown, Peter");
        // Method description is different than code -> additional comma
        // there
        assertEquals("von Neumann, John, Smith, John and Black Brown, Peter",
                al.getAsLastFirstNames(false, false));
        assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", al.getAsLastFirstNames(true, false));

        al = AuthorList.parse("John Peter von Neumann");
        assertEquals("von Neumann, J. P.", al.getAsLastFirstNames(true, false));
    }

    @Test
    public void testGetAuthorsLastFirstOxfordComma() {
        // Oxford comma
        AuthorList al;

        al = AuthorList.parse("");
        assertEquals("", al.getAsLastFirstNames(true, true));
        assertEquals("", al.getAsLastFirstNames(false, true));

        al = AuthorList.parse("John Smith");
        assertEquals("Smith, John", al.getAsLastFirstNames(false, true));
        assertEquals("Smith, J.", al.getAsLastFirstNames(true, true));

        al = AuthorList.parse("John Smith and Black Brown, Peter");
        assertEquals("Smith, John and Black Brown, Peter", al.getAsLastFirstNames(false, true));
        assertEquals("Smith, J. and Black Brown, P.", al.getAsLastFirstNames(true, true));

        al = AuthorList.parse("John von Neumann and John Smith and Black Brown, Peter");
        assertEquals("von Neumann, John, Smith, John, and Black Brown, Peter", al
                .getAsLastFirstNames(false, true));
        assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", al.getAsLastFirstNames(
                true, true));

        al = AuthorList.parse("John Peter von Neumann");
        assertEquals("von Neumann, J. P.", al.getAsLastFirstNames(true, true));
    }

    @Test
    public void testGetAuthorsLastFirstAnds() {
        assertEquals("Smith, John", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(
                false));
        assertEquals("Smith, John and Black Brown, Peter", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsLastFirstNamesWithAnd(false));
        assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
                .parse("John von Neumann and John Smith and Black Brown, Peter")
                .getAsLastFirstNamesWithAnd(false));
        assertEquals("von Last, Jr, First", AuthorList.parse("von Last, Jr ,First")
                                                      .getAsLastFirstNamesWithAnd(false));

        assertEquals("Smith, J.", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(
                true));
        assertEquals("Smith, J. and Black Brown, P.", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsLastFirstNamesWithAnd(true));
        assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsLastFirstNamesWithAnd(true));
        assertEquals("von Last, Jr, F.", AuthorList.parse("von Last, Jr ,First")
                                                   .getAsLastFirstNamesWithAnd(true));
    }

    @Test
    public void testGetAuthorsLastFirstAndsCaching() {
        // getAsLastFirstNamesWithAnd caches its results, therefore we call the method twice using the same arguments
        assertEquals("Smith, John", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(false));
        assertEquals("Smith, John", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(false));
        assertEquals("Smith, J.", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(true));
        assertEquals("Smith, J.", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(true));
    }

    @Test
    public void testGetAuthorsFirstFirst() {

        AuthorList al;

        al = AuthorList.parse("");
        assertEquals("", al.getAsFirstLastNames(true, false));
        assertEquals("", al.getAsFirstLastNames(false, false));
        assertEquals("", al.getAsFirstLastNames(true, true));
        assertEquals("", al.getAsFirstLastNames(false, true));

        al = AuthorList.parse("John Smith");
        assertEquals("John Smith", al.getAsFirstLastNames(false, false));
        assertEquals("J. Smith", al.getAsFirstLastNames(true, false));
        assertEquals("John Smith", al.getAsFirstLastNames(false, true));
        assertEquals("J. Smith", al.getAsFirstLastNames(true, true));

        al = AuthorList.parse("John Smith and Black Brown, Peter");
        assertEquals("John Smith and Peter Black Brown", al.getAsFirstLastNames(false, false));
        assertEquals("J. Smith and P. Black Brown", al.getAsFirstLastNames(true, false));
        assertEquals("John Smith and Peter Black Brown", al.getAsFirstLastNames(false, true));
        assertEquals("J. Smith and P. Black Brown", al.getAsFirstLastNames(true, true));

        al = AuthorList.parse("John von Neumann and John Smith and Black Brown, Peter");
        assertEquals("John von Neumann, John Smith and Peter Black Brown", al.getAsFirstLastNames(
                false, false));
        assertEquals("J. von Neumann, J. Smith and P. Black Brown", al.getAsFirstLastNames(true,
                false));
        assertEquals("John von Neumann, John Smith, and Peter Black Brown", al
                .getAsFirstLastNames(false, true));
        assertEquals("J. von Neumann, J. Smith, and P. Black Brown", al.getAsFirstLastNames(true,
                true));

        al = AuthorList.parse("John Peter von Neumann");
        assertEquals("John Peter von Neumann", al.getAsFirstLastNames(false, false));
        assertEquals("John Peter von Neumann", al.getAsFirstLastNames(false, true));
        assertEquals("J. P. von Neumann", al.getAsFirstLastNames(true, false));
        assertEquals("J. P. von Neumann", al.getAsFirstLastNames(true, true));
    }

    @Test
    public void testGetAuthorsFirstFirstAnds() {
        assertEquals("John Smith", AuthorList.parse("John Smith")
                                             .getAsFirstLastNamesWithAnd());
        assertEquals("John Smith and Peter Black Brown", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsFirstLastNamesWithAnd());
        assertEquals("John von Neumann and John Smith and Peter Black Brown", AuthorList
                .parse("John von Neumann and John Smith and Black Brown, Peter")
                .getAsFirstLastNamesWithAnd());
        assertEquals("First von Last, Jr. III", AuthorList
                .parse("von Last, Jr. III, First").getAsFirstLastNamesWithAnd());
    }

    @Test
    public void testGetAuthorsForAlphabetization() {
        assertEquals("Smith, J.", AuthorList.parse("John Smith")
                                            .getForAlphabetization());
        assertEquals("Neumann, J.", AuthorList.parse("John von Neumann")
                                              .getForAlphabetization());
        assertEquals("Neumann, J.", AuthorList.parse("J. von Neumann")
                                              .getForAlphabetization());
        assertEquals("Neumann, J. and Smith, J. and Black Brown, Jr., P.", AuthorList
                .parse("John von Neumann and John Smith and de Black Brown, Jr., Peter")
                .getForAlphabetization());
    }

    @Test
    public void testRemoveStartAndEndBraces() {
        assertEquals("{A}bbb{c}", AuthorList.parse("{A}bbb{c}").getAsLastNames(false));
        assertEquals("Vall{\\'e}e Poussin", AuthorList.parse("{Vall{\\'e}e Poussin}").getAsLastNames(false));
        assertEquals("Poussin", AuthorList.parse("{Vall{\\'e}e} {Poussin}").getAsLastNames(false));
        assertEquals("Poussin", AuthorList.parse("Vall{\\'e}e Poussin").getAsLastNames(false));
        assertEquals("Lastname", AuthorList.parse("Firstname {Lastname}").getAsLastNames(false));
        assertEquals("Firstname Lastname", AuthorList.parse("{Firstname Lastname}").getAsLastNames(false));
    }

    @Test
    public void createCorrectInitials() {
        assertEquals(Optional.of("J. G."),
                AuthorList.parse("Hornberg, Johann Gottfried").getAuthor(0).getFirstAbbr());
    }

    @Test
    public void parseNameWithBracesAroundFirstName() throws Exception {
        // TODO: Be more intelligent and abbreviate the first name correctly
        Author expected = new Author("Tse-tung", "{Tse-tung}.", null, "Mao", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("{Tse-tung} Mao"));
    }

    @Test
    public void parseNameWithBracesAroundLastName() throws Exception {
        Author expected = new Author("Hans", "H.", null, "van den Bergen", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("{van den Bergen}, Hans"));
    }

    @Test
    public void parseNameWithHyphenInFirstName() throws Exception {
        Author expected = new Author("Tse-tung", "T.-t.", null, "Mao", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("Tse-tung Mao"));
    }

    @Test
    public void parseNameWithHyphenInLastName() throws Exception {
        Author expected = new Author("Firstname", "F.", null, "Bailey-Jones", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("Firstname Bailey-Jones"));
    }

    @Test
    public void parseNameWithHyphenInLastNameWithInitials() throws Exception {
        Author expected = new Author("E. S.", "E. S.", null, "El-{M}allah", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("E. S. El-{M}allah"));
    }

    @Test
    public void parseNameWithHyphenInLastNameWithEscaped() throws Exception {
        Author expected = new Author("E. S.", "E. S.", null, "{K}ent-{B}oswell", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("E. S. {K}ent-{B}oswell"));
    }

    @Test
    public void parseNameWithHyphenInLastNameWhenLastNameGivenFirst() throws Exception {
        // TODO: Fix abbreviation to be "A."
        Author expected = new Author("ʿAbdallāh", "ʿ.", null, "al-Ṣāliḥ", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("al-Ṣāliḥ, ʿAbdallāh"));
    }

    @Test
    public void parseNameWithBraces() throws Exception {
        Author expected = new Author("H{e}lene", "H.", null, "Fiaux", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("H{e}lene Fiaux"));
    }

    /**
     * This tests the issue described at https://github.com/JabRef/jabref/pull/2669#issuecomment-288519458
     */
    @Test
    public void correctNamesWithOneComma() throws Exception {
        Author expected = new Author("Alexander der Große", "A. d. G.", null, "Canon der Barbar", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("Canon der Barbar, Alexander der Große"));

        expected = new Author("Alexander H. G.", "A. H. G.", null, "Rinnooy Kan", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("Rinnooy Kan, Alexander H. G."));

        expected = new Author("Alexander Hendrik George", "A. H. G.", null, "Rinnooy Kan", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("Rinnooy Kan, Alexander Hendrik George"));

        expected = new Author("José María", "J. M.", null, "Rodriguez Fernandez", null);
        assertEquals(new AuthorList(expected), AuthorList.parse("Rodriguez Fernandez, José María"));
    }
}
