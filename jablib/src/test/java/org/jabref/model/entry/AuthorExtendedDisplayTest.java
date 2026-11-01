package org.jabref.model.entry;

import java.util.Map;

import org.jabref.logic.bibtex.comparator.NameComparator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.jspecify.annotations.NullMarked;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class AuthorExtendedDisplayTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            false | Hasselt     | Hasselt, Hado P. van     | Hasselt, HP van
            true  | van Hasselt | van Hasselt, Hado P.     | van Hasselt, HP
            """)
    void prefixOptionControlsFamilyAndInvertedViews(String option, String family, String inverted, String abbreviated) {
        Author author = AuthorList.parse("family=Hasselt, given=Hado P., given-i=HP, prefix=van, useprefix=" + option).getAuthor(0);

        assertEquals(family, author.getFamilyNameForDisplay());
        assertEquals(inverted, author.getFamilyGiven(false));
        assertEquals(abbreviated, author.getFamilyGiven(true));
        assertEquals("Hado P. van Hasselt", author.getGivenFamily(false));
        assertEquals("HP van Hasselt", author.getGivenFamily(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ", useprefix=invalid"})
    void absentAndInvalidOptionsKeepExistingPrefixInclusiveDisplay(String option) {
        Author author = AuthorList.parse("family=Hasselt, given=Hado P., prefix=van" + option).getAuthor(0);

        assertEquals("van Hasselt", author.getFamilyNameForDisplay());
        assertEquals("van Hasselt, Hado P.", author.getFamilyGiven(false));
    }

    @Test
    void explicitEmptyInitialsSuppressGeneratedInitials() {
        Author author = AuthorList.parse("family=Hasselt, given=Hado P., given-i={}, prefix=van, useprefix=false").getAuthor(0);

        assertEquals("van Hasselt", author.getGivenFamily(true));
        assertEquals("Hasselt, van", author.getFamilyGiven(true));
        assertEquals("Hado P. van Hasselt", author.getGivenFamily(false));
    }

    @Test
    void explicitInitialsOverrideGeneratedInitialsInManuallyConstructedAuthors() {
        Author author = new Author("Hado P.", "H. P.", "van", "Hasselt", "",
                Map.of("family", "Hasselt", "given-i", "{HP}"));

        assertEquals("HP van Hasselt", author.getGivenFamily(true));
    }

    @Test
    void familyFirstViewKeepsSuffixAndMovesExcludedPrefixAfterGivenName() {
        Author author = AuthorList.parse("family=Hasselt, given=Hado, suffix=Jr., prefix=van, useprefix=false").getAuthor(0);

        assertEquals("Hasselt, Jr., Hado van", author.getFamilyGiven(false));
        assertEquals("Hado van Hasselt, Jr.", author.getGivenFamily(false));
    }

    @Test
    void apostrophePrefixIsJoinedWithoutAnExtraSpace() {
        Author author = AuthorList.parse("family=Antin, given=Guy, prefix=d’, useprefix=true").getAuthor(0);

        assertEquals("d’Antin", author.getFamilyNameForDisplay());
        assertEquals("Guy d’Antin", author.getGivenFamily(false));
    }

    @Test
    void nameListsApplyOptionsToEachPersonIncludingNatbibLabels() {
        AuthorList authors = AuthorList.parse("family=Hasselt, prefix=van, useprefix=false"
                + " and family=Ree, prefix=van der, useprefix=true");

        assertEquals("Hasselt and van der Ree", authors.getAsLastNames(false));
        assertEquals("Hasselt and van der Ree", authors.getAsNatbib());
        assertEquals("Hasselt et al.", AuthorList.parse(authors.getAsBibtex() + " and Guez, Arthur").getAsNatbib());
    }

    @Test
    void viewingDoesNotModifyStorageOrSortOrder() {
        String input = "family=Hasselt, given=Hado P., given-i={HP}, prefix=van, useprefix=false";
        Author author = AuthorList.parse(input).getAuthor(0);
        String stored = author.getAsBibtex();
        author.getFamilyNameForDisplay();
        author.getFamilyGiven(true);
        author.getGivenFamily(false);
        author.latexFree().getGivenFamily(true);

        assertEquals(stored, author.getAsBibtex());
        assertEquals(-1, Integer.signum(new NameComparator().compare(stored, "Miller, Max")));
    }

    @Test
    void conventionalNamesKeepTheirDisplayOutput() {
        Author author = AuthorList.parse("van Hasselt, Hado P.").getAuthor(0);

        assertEquals("van Hasselt", author.getFamilyNameForDisplay());
        assertEquals("van Hasselt, Hado P.", author.getFamilyGiven(false));
        assertEquals("H. P. van Hasselt", author.getGivenFamily(true));
    }
}
