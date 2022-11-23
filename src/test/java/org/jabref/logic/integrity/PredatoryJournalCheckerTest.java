package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PredatoryJournalCheckerTest {

    private static PredatoryJournalChecker checker;
    private BibEntry entry;

    @BeforeAll
    static void initChecker() {
        checker = new PredatoryJournalChecker(StandardField.JOURNAL, StandardField.PUBLISHER, StandardField.BOOKTITLE);
    }

    @BeforeEach
    void initEntry() {
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
        assertEquals(List.of(new IntegrityMessage("match found in predatory journal list", entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void journalIsPredatoryCaseInsensitive() {
        entry.setField(StandardField.JOURNAL, "european international journal of science and technology");
        assertEquals(List.of(new IntegrityMessage("match found in predatory journal list", entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void journalIsPredatoryExtraCharacters() {
        entry.setField(StandardField.JOURNAL, "European International Journal, of Science and Technology");
        assertEquals(List.of(new IntegrityMessage("match found in predatory journal list", entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void publisherIsPredatory() {
        entry.setField(StandardField.PUBLISHER, "Academia Scholarly Journals");
        assertEquals(List.of(new IntegrityMessage("match found in predatory journal list", entry, StandardField.PUBLISHER)), checker.check(entry));
    }

    @Test
    void booktitleIsPredatory() {
        entry.setField(StandardField.BOOKTITLE, "Biosciences International");
        assertEquals(List.of(new IntegrityMessage("match found in predatory journal list", entry, StandardField.BOOKTITLE)), checker.check(entry));
    }
}
