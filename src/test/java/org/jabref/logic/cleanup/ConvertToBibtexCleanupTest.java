package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConvertToBibtexCleanupTest {

    private ConvertToBibtexCleanup worker;

    @BeforeEach
    public void setUp() {
        worker = new ConvertToBibtexCleanup();
    }

    @Test
    public void cleanupMovesDateToYearAndMonth() {
        BibEntry entry = new BibEntry().withField(StandardField.DATE, "2011-01");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.DATE));
        assertEquals(Optional.of("2011"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("#jan#"), entry.getField(StandardField.MONTH));
    }

    @Test
    public void cleanupWithYearAlreadyPresentDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.YEAR, "2011");
        entry.setField(StandardField.DATE, "2012");

        worker.cleanup(entry);

        assertEquals(Optional.of("2011"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("2012"), entry.getField(StandardField.DATE));
    }

    @Test
    public void cleanupMovesJournaltitleToJournal() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNALTITLE, "Best of JabRef");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.JOURNALTITLE));
        assertEquals(Optional.of("Best of JabRef"), entry.getField(StandardField.JOURNAL));
    }

    @Test
    public void cleanUpDoesntMoveFileField() {
        String fileField = ":Ambriola2006 - On the Systematic Analysis of Natural Language Requirements with CIRCE.pdf:PDF";
        BibEntry entry = new BibEntry().withField(StandardField.FILE, fileField);

        worker.cleanup(entry);

        assertEquals(Optional.of(fileField), entry.getField(StandardField.FILE));
    }
}
