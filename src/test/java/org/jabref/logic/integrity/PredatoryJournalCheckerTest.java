package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PredatoryJournalCheckerTest {

    private PredatoryJournalChecker checker;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        checker = new PredatoryJournalChecker(StandardField.JOURNAL);
        entry = new BibEntry();
    }

    @Test
    void journalIsNotPredatory() {
        entry.setField(StandardField.JOURNAL, "IEEE Software");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void journalIsPredatory() {
        entry.setField(StandardField.JOURNAL, "European International Journal of Science and Technology");
        assertEquals(List.of(new IntegrityMessage("journal match found in predatory journal list", entry, StandardField.JOURNAL)), checker.check(entry));
    }
}
