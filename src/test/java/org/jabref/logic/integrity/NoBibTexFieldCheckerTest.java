package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoBibTexFieldCheckerTest {

    private final NoBibtexFieldChecker checker = new NoBibtexFieldChecker();

    @Test
    void abstractIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("abstract", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void addressIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("address", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void afterwordIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("afterword", "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, "afterword");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    void arbitraryNonBiblatexFieldIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("fieldNameNotDefinedInThebiblatexManual", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void commentIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("comment", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void instituationIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("institution", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void journalIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void journaltitleIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("journaltitle", "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, "journaltitle");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    void keywordsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void locationIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("location", "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, "location");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    void reviewIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("review", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }
}
