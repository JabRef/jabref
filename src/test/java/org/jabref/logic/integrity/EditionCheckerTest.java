package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditionCheckerTest {

    public BibDatabaseContext bibDatabaseContextEdition = new BibDatabaseContext();

    @Test
    void isFirstCharacterANumber() {
        boolean allowIntegerEdition = false;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertTrue(editionChecker.isFirstCharDigit("0HelloWorld"));
    }

    @Test
    void isFirstCharacterANumberFalseForEmptyString() {
        boolean allowIntegerEdition = false;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertFalse(editionChecker.isFirstCharDigit(""));
    }

    @Test
    void isFirstCharacterNotANumber() {
        boolean allowIntegerEdition = false;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertFalse(editionChecker.isFirstCharDigit("HelloWorld"));
    }

    @Test
    void editionCheckerDoesNotComplainIfAllowIntegerEditionIsEnabled() {
        boolean allowIntegerEdition = true;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertEquals(Optional.empty(), editionChecker.checkValue("2"));
    }

}
