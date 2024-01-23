package org.jabref.logic.integrity;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnicodeNormalFormCCheckTest {
    UnicodeNormalFormCCheck checker = new UnicodeNormalFormCCheck();
    BibEntry entry = new BibEntry();

    @Test
    void checkWithNormalizedStringShouldReturnEmptyList() {
        entry.setField(StandardField.TITLE, "Some Title");
        entry.setField(StandardField.AUTHOR, "John Doe");

        assertTrue(checker.check(entry).isEmpty());
    }

    @Test
    void checkWithNonNormalizedStringShouldReturnIntegrityMessage() {
        entry.setField(StandardField.TITLE, "Café");
        entry.setField(StandardField.AUTHOR, "John Doe");

        assertFalse(checker.check(entry).isEmpty());
    }
}
