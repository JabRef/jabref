package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class NoBibTexFieldCheckerTest {

    private final NoBibtexFieldChecker checker = new NoBibtexFieldChecker();

    @Test
    public void abstractIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("abstract", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void addressIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("address", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void afterwordIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("afterword", "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, "afterword");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    public void arbitraryNonBiblatexFieldIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("fieldNameNotDefinedInThebiblatexManual", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void commentIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("comment", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void instituationIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("institution", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void journalIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void journaltitleIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("journaltitle", "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, "journaltitle");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    public void keywordsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void locationIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("location", "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, "location");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    public void reviewIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("review", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

}
