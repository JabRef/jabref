package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.journals.predatory.PredatoryJournalListLoader;
import org.jabref.logic.journals.predatory.PredatoryJournalRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PredatoryJournalCheckerTest {

    static PredatoryJournalChecker checker;
    static PredatoryJournalRepository predatoryJournalRepository = PredatoryJournalListLoader.loadRepository();

    @BeforeAll
    static void initChecker() {
        checker = new PredatoryJournalChecker(predatoryJournalRepository,
                List.of(StandardField.JOURNAL, StandardField.PUBLISHER, StandardField.BOOKTITLE));
    }

    @AfterAll
    static void close() throws Exception {
        predatoryJournalRepository.close();
    }

    @Test
    void journalIsNotPredatory() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "IEEE Software");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void journalIsPredatory() {
        String journalName = "European International Journal of Science and Technology";
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, journalName);
        assertEquals(List.of(new IntegrityMessage("Predatory journal %s found".formatted(journalName),
                entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void journalIsPredatoryCaseInsensitive() {
        String journalName = "european international journal of science and technology";
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, journalName);
        assertEquals(List.of(new IntegrityMessage("Predatory journal %s found".formatted(journalName),
                entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void journalIsPredatoryExtraCharacters() {
        String journalName = "European International Journal, of Science and Technology";
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, journalName);
        assertEquals(List.of(new IntegrityMessage("Predatory journal %s found".formatted(journalName),
                entry, StandardField.JOURNAL)), checker.check(entry));
    }

    @Test
    void publisherIsPredatory() {
        String publisherName = "Academia Scholarly Journals";
        BibEntry entry = new BibEntry().withField(StandardField.PUBLISHER, publisherName);
        assertEquals(List.of(new IntegrityMessage("Predatory journal %s found".formatted(publisherName),
                entry, StandardField.PUBLISHER)), checker.check(entry));
    }

    @Test
    void bookTitleIsPredatory() {
        String bookTitle = "Biosciences International";
        BibEntry entry = new BibEntry().withField(StandardField.BOOKTITLE, bookTitle);
        assertEquals(List.of(new IntegrityMessage("Predatory journal %s found".formatted(bookTitle),
                entry, StandardField.BOOKTITLE)), checker.check(entry));
    }
}
