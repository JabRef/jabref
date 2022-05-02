package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditionCheckerTest {
    @Test
    void isFirstCharacterANumber() {
        assertTrue(createSimpleEditionChecker(new BibDatabaseContext(), false).isFirstCharDigit("0HelloWorld"));
    }

    @Test
    void isFirstCharacterANumberFalseForEmptyString() {
        assertFalse(createSimpleEditionChecker(new BibDatabaseContext(), false).isFirstCharDigit(""));
    }

    @Test
    void isFirstCharacterNotANumber() {
        assertFalse(createSimpleEditionChecker(new BibDatabaseContext(), false).isFirstCharDigit("HelloWorld"));
    }

    @Test
    void editionCheckerDoesNotComplainIfAllowIntegerEditionIsEnabled() {
        assertEquals(Optional.empty(), createSimpleEditionChecker(new BibDatabaseContext(), true).checkValue("2"));
    }

    @Test
    void bibTexAcceptsOrdinalNumberInWordsWithCapitalFirstLetter() {
        assertEquals(Optional.empty(), createBibtexEditionChecker(true).checkValue("Second"));
    }

    @Test
    void bibTexDoesNotAcceptOrdinalNumberInWordsWithNonCapitalFirstLetter() {
        assertNotEquals(Optional.empty(), createBibtexEditionChecker(true).checkValue("second"));
    }

    @Test
    void bibTexAcceptsIntegerInputInEdition() {
        assertEquals(Optional.empty(), createBibtexEditionChecker(true).checkValue("2"));
    }

    @Test
    void bibTexAcceptsOrdinalNumberInNumbers() {
        assertEquals(Optional.empty(), createBibtexEditionChecker(true).checkValue("2nd"));
    }

    @Test
    void bibTexEmptyValueAsInput() {
        assertEquals(Optional.empty(), createBibtexEditionChecker(false).checkValue(""));
    }

    @Test
    void bibTexNullValueAsInput() {
        assertEquals(Optional.empty(), createBibtexEditionChecker(false).checkValue(null));
    }

    @Test
    void bibTexDoesNotAcceptIntegerOnly() {
        assertEquals(Optional.of(Localization.lang("no integer as values for edition allowed")), createBibtexEditionChecker(false).checkValue("3"));
    }

    @Test
    void bibTexAcceptsFirstEditionAlsoIfIntegerEditionDisallowed() {
        assertEquals(Optional.of(Localization.lang("edition of book reported as just 1")), createBibtexEditionChecker(false).checkValue("1"));
    }

    @Test
    void bibLaTexAcceptsEditionWithCapitalFirstLetter() {
        assertEquals(Optional.empty(), createBiblatexEditionChecker(true).checkValue("Edition 2000"));
    }

    @Test
    void bibLaTexAcceptsIntegerInputInEdition() {
        assertEquals(Optional.empty(), createBiblatexEditionChecker(true).checkValue("2"));
    }

    @Test
    void bibLaTexAcceptsEditionAsLiteralString() {
        assertEquals(Optional.empty(), createBiblatexEditionChecker(true).checkValue("Third, revised and expanded edition"));
    }

    @Test
    void bibLaTexDoesNotAcceptOrdinalNumberInNumbers() {
        assertNotEquals(Optional.empty(), createBiblatexEditionChecker(true).checkValue("2nd"));
    }

    private EditionChecker createBibtexEditionChecker(Boolean allowIntegerEdition) {
        BibDatabaseContext bibtex = new BibDatabaseContext();
        bibtex.setMode(BibDatabaseMode.BIBTEX);
        return new EditionChecker(bibtex, allowIntegerEdition);
    }

    private EditionChecker createBiblatexEditionChecker(Boolean allowIntegerEdition) {
        BibDatabaseContext biblatex = new BibDatabaseContext();
        biblatex.setMode(BibDatabaseMode.BIBLATEX);
        return new EditionChecker(biblatex, allowIntegerEdition);
    }

    private EditionChecker createSimpleEditionChecker(BibDatabaseContext bibDatabaseContextEdition, boolean allowIntegerEdition) {
        return new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
    }
}
