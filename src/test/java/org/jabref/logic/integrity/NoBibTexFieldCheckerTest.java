package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoBibTexFieldCheckerTest {

    private final NoBibtexFieldChecker checker = new NoBibtexFieldChecker();

    @Test
    void abstractIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.ABSTRACT, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void addressIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.ADDRESS, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void afterwordIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AFTERWORD, "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, StandardField.AFTERWORD);
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    void arbitraryNonBiblatexFieldIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(new UnknownField("fieldNameNotDefinedInThebiblatexManual"), "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void commentIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.COMMENT, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void instituationIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.INSTITUTION, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void journalIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.JOURNAL, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void journaltitleIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.JOURNALTITLE, "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, StandardField.JOURNALTITLE);
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    void keywordsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    void locationIsRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.LOCATION, "test");
        IntegrityMessage message = new IntegrityMessage("biblatex field only", entry, StandardField.LOCATION);
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    void reviewIsNotRecognizedAsBiblatexOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.REVIEW, "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }
}
