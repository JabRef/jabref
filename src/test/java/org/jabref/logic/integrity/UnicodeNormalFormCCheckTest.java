package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UnicodeNormalFormCCheckTest {
    UnicodeNormalFormCCheck checker = new UnicodeNormalFormCCheck();
    BibEntry entry = new BibEntry();

    @Test
    void checkWithNormalizedStringShouldReturnEmptyList() {
        entry.setField(StandardField.TITLE, "Some Title");
        entry.setField(StandardField.AUTHOR, "John Doe");

        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void checkWithNonNormalizedStringShouldReturnIntegrityMessage() {
        entry.setField(StandardField.TITLE, "Café");
        entry.setField(StandardField.AUTHOR, "John Doe");

        assertFalse(checker.check(entry).isEmpty());
        assertEquals(List.of(new IntegrityMessage("Value is not in Normal Form C (NFC) format", entry, StandardField.TITLE)), checker.check(entry));
    }
}
