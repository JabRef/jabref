package org.jabref.model.entry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorTest {
    @Test
    void absentUsePrefixDoesNotImposeADefault() {
        Author author = new Author("Hado P.", "H. P.", "van", "Hasselt", "");

        assertEquals(Optional.empty(), author.getUsePrefix());
        assertEquals(Optional.empty(), author.getInvalidUsePrefix());
    }

    @Test
    void readsExplicitInitialsWithoutGeneratingOrRepunctuatingThem() {
        Map<String, String> parts = Map.of("family", "Hasselt", "given-i", "{HP}",
                "family-i", "H", "prefix-i", "v.", "suffix-i", "{Jr}");
        Author author = new Author("Hado P.", "H. P.", "van", "Hasselt", "Junior", parts);

        assertEquals(Optional.of("HP"), author.getExplicitGivenNameInitials());
        assertEquals(Optional.of("H"), author.getExplicitFamilyNameInitials());
        assertEquals(Optional.of("v."), author.getExplicitNamePrefixInitials());
        assertEquals(Optional.of("Jr"), author.getExplicitNameSuffixInitials());
        assertEquals(parts, author.getExtendedNameParts());
        assertEquals(Optional.of("H. P."), author.getGivenNameAbbreviated());
    }

    @Test
    void absentInitialsRemainDistinctFromExplicitlyEmptyInitials() {
        Author conventional = new Author("Hado P.", "H. P.", "van", "Hasselt", "");
        Author extended = new Author("Hado P.", "H. P.", "van", "Hasselt", "",
                Map.of("family", "Hasselt", "given-i", "{}"));

        assertEquals(Optional.empty(), conventional.getExplicitGivenNameInitials());
        assertEquals(Optional.empty(), conventional.getExplicitFamilyNameInitials());
        assertEquals(Optional.empty(), conventional.getExplicitNamePrefixInitials());
        assertEquals(Optional.empty(), conventional.getExplicitNameSuffixInitials());
        assertEquals(Optional.of(""), extended.getExplicitGivenNameInitials());
    }

    @Test
    void parserKeepsOptionsAndInitialsSeparateForEachAuthor() {
        AuthorList authors = AuthorList.parse("family=Hasselt, given=Hado P., prefix=van, given-i=HP, useprefix=false"
                + " and family=Ree, given=Michiel, prefix=van der, given-i=M, useprefix=true and Guez, Arthur");

        assertEquals(Optional.of(false), authors.getAuthor(0).getUsePrefix());
        assertEquals(Optional.of("HP"), authors.getAuthor(0).getExplicitGivenNameInitials());
        assertEquals(Optional.of(true), authors.getAuthor(1).getUsePrefix());
        assertEquals(Optional.of("M"), authors.getAuthor(1).getExplicitGivenNameInitials());
        assertEquals(Optional.empty(), authors.getAuthor(2).getUsePrefix());
        assertEquals(authors, AuthorList.parse(authors.getAsBibtex()));
        assertEquals(Optional.of("HP"), authors.latexFree().getAuthor(0).getExplicitGivenNameInitials());
    }

    @Test
    void differentNameOptionsDistinguishAuthors() {
        Author withoutPrefix = AuthorList.parse("family=Hasselt, prefix=van, useprefix=false").getAuthor(0);
        Author withPrefix = AuthorList.parse("family=Hasselt, prefix=van, useprefix=true").getAuthor(0);
        Author reparsed = AuthorList.parse(withoutPrefix.getAsBibtex()).getAuthor(0);

        assertNotEquals(withoutPrefix, withPrefix);
        assertEquals(withoutPrefix, reparsed);
        assertEquals(withoutPrefix.hashCode(), reparsed.hashCode());
    }

    @Test
    void latexFreeDisplayRetainsExtendedPartsForStorage() {
        Author author = AuthorList.parse("family={García}, given=José, useprefix=false").getAuthor(0);

        assertEquals(author.getExtendedNameParts(), author.latexFree().getExtendedNameParts());
        assertEquals(author.getAsBibtex(), author.latexFree().getAsBibtex());
    }

    @Test
    void conventionalNamesKeepTheirStorageFormat() {
        Author author = new Author("John", "J.", "von", "Smith", "Jr.");

        assertEquals(Map.of(), author.getExtendedNameParts());
        assertEquals("von Smith, Jr., John", author.getAsBibtex());
    }

    @ParameterizedTest
    @CsvSource(
            """
                    AO, A. O.
                    AO., A. O.
                    A.O., A. O.
                    A-O, A.-O.
                    """
    )
    void addDotIfAbbreviationAddsDot(String input, String expected) {
        assertEquals(expected, Author.addDotIfAbbreviation(input));
    }

    @ParameterizedTest
    @CsvSource("""
            A O, A. O.
            A-melia, A.-melia
            AmeliA, AmeliA
            Ameli A, Ameli A.
            Ameli , Ameli
            Ameli AA, Ameli A. A.
            """
    )
    void addDotIfAbbreviationEdgeCases(String input, String expected) {
        assertEquals(expected, Author.addDotIfAbbreviation(input));
    }

    @ParameterizedTest
    @CsvSource(delimiterString = "->", textBlock = """
            O.
            A. O.
            A.-O.
            O. Moore
            A. O. Moore
            O. von Moore
            A.-O. Moore
            Moore, O.
            Moore, O., Jr.
            Moore, A. O.
            Moore, A.-O.
            MEmre
            {\\\\'{E}}douard
            J{\\\\\\"o}rg
            Moore, O. and O. Moore
            Moore, O. and O. Moore and Moore, O. O.
            """)
    void addDotIfAbbreviationDoNotAddDot(String input) {
        assertEquals(input, Author.addDotIfAbbreviation(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            // Lower-case letters
            "asdf",
            "a",
            // Numbers
            "1",
            "1 23"
    })
    void addDotIfAbbreviation(String input) {
        assertEquals(input, Author.addDotIfAbbreviation(input));
    }

    @Test
    void bracesKept() {
        assertEquals(Optional.of("{Company Name, LLC}"),
                new Author("", "", null, "{Company Name, LLC}", null).getFamilyName());
    }
}
