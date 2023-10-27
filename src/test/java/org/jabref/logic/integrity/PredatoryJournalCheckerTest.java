package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.journals.PredatoryJournalLoader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredatoryJournalCheckerTest {

    static PredatoryJournalChecker checker;
    BibEntry entry;

    @BeforeAll
    static void initChecker() {
        checker = new PredatoryJournalChecker(PredatoryJournalLoader.loadRepository(),
                List.of(StandardField.JOURNAL, StandardField.PUBLISHER, StandardField.BOOKTITLE));
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
        String journalName = "European International Journal of Science and Technology";
        entry.setField(StandardField.JOURNAL, journalName);
        assertEquals(List.of(new IntegrityMessage("Match found in predatory journal " + journalName,
                entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void journalIsPredatoryCaseInsensitive() {
        String journalName = "european international journal of science and technology";
        entry.setField(StandardField.JOURNAL, journalName);
        assertEquals(List.of(new IntegrityMessage("Match found in predatory journal " + journalName,
                entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void journalIsPredatoryExtraCharacters() {
        String journalName = "European International Journal, of Science and Technology";
        entry.setField(StandardField.JOURNAL, journalName);
        assertEquals(List.of(new IntegrityMessage("Match found in predatory journal " + journalName,
                entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void publisherIsPredatory() {
        String publisherName = "Academia Scholarly Journals";
        entry.setField(StandardField.PUBLISHER, publisherName);
        assertEquals(List.of(new IntegrityMessage("Match found in predatory journal " + publisherName,
                entry, StandardField.PUBLISHER)), checker.check(entry));
    }

    @Test
    void bookTitleIsPredatory() {
        String bookTitle = "Biosciences International";
        entry.setField(StandardField.BOOKTITLE, bookTitle);
        assertEquals(List.of(new IntegrityMessage("Match found in predatory journal " + bookTitle,
                entry, StandardField.BOOKTITLE)), checker.check(entry));
    }
}
