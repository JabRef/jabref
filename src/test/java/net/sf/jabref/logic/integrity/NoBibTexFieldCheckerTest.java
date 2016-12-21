package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class NoBibTexFieldCheckerTest {

    private NoBibtexFieldChecker checker = new NoBibtexFieldChecker();

    @Test
    public void abstractIsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("abstract", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void addressIsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("address", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void afterwordIsRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("afterword", "test");
        IntegrityMessage message = new IntegrityMessage("BibLaTeX field only", entry, "afterword");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    public void arbitraryNonBibLaTeXFieldIsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("fieldNameNotDefinedInTheBibLaTeXManual", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void commentIsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("comment", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void instituationIsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("institution", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void journalIsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("journal", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void journaltitleIsRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("journaltitle", "test");
        IntegrityMessage message = new IntegrityMessage("BibLaTeX field only", entry, "journaltitle");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    public void keywordsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("keywords", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

    @Test
    public void locationIsRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("location", "test");
        IntegrityMessage message = new IntegrityMessage("BibLaTeX field only", entry, "location");
        List<IntegrityMessage> messages = checker.check(entry);
        assertEquals(Collections.singletonList(message), messages);
    }

    @Test
    public void reviewIsNotRecognizedAsBibLaTeXOnlyField() {
        BibEntry entry = new BibEntry();
        entry.setField("review", "test");
        assertEquals(Collections.emptyList(), checker.check(entry));
    }

}
