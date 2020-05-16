package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConvertToBiblatexCleanupTest {

    private ConvertToBiblatexCleanup worker;

    @BeforeEach
    public void setUp() {
        worker = new ConvertToBiblatexCleanup();
    }

    @Test
    public void cleanupMovesYearMonthToDate() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.YEAR, "2011");
        entry.setField(StandardField.MONTH, "#jan#");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.YEAR));
        assertEquals(Optional.empty(), entry.getField(StandardField.MONTH));
        assertEquals(Optional.of("2011-01"), entry.getField(StandardField.DATE));
    }

    @Test
    public void cleanupWithDateAlreadyPresentAndDifferentFromYearDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.YEAR, "2011");
        entry.setField(StandardField.MONTH, "#jan#");
        entry.setField(StandardField.DATE, "2012-01");

        worker.cleanup(entry);

        assertEquals(Optional.of("2011"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("#jan#"), entry.getField(StandardField.MONTH));
        assertEquals(Optional.of("2012-01"), entry.getField(StandardField.DATE));
    }

    @Test
    public void cleanupWithDateAlreadyPresentAndDifferentFromMonthDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.YEAR, "2011");
        entry.setField(StandardField.MONTH, "#jan#");
        entry.setField(StandardField.DATE, "2011-02");

        worker.cleanup(entry);

        assertEquals(Optional.of("2011"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("#jan#"), entry.getField(StandardField.MONTH));
        assertEquals(Optional.of("2011-02"), entry.getField(StandardField.DATE));
    }

    @Test
    public void cleanupWithEmptyDateDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.YEAR, "");
        entry.setField(StandardField.MONTH, "");
        entry.setField(StandardField.DATE, "");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.YEAR));
        assertEquals(Optional.empty(), entry.getField(StandardField.MONTH));
        assertEquals(Optional.empty(), entry.getField(StandardField.DATE));
    }

    @Test
    public void cleanupWithDateAlreadyPresentAndEqualsToYearAndMonth() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.YEAR, "2011");
        entry.setField(StandardField.MONTH, "#jan#");
        entry.setField(StandardField.DATE, "2011-01");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.YEAR));
        assertEquals(Optional.empty(), entry.getField(StandardField.MONTH));
        assertEquals(Optional.of("2011-01"), entry.getField(StandardField.DATE));
    }

    @Test
    public void cleanupMovesJournalToJournaltitle() {
        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Best of JabRef");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Best of JabRef"), entry.getField(StandardField.JOURNALTITLE));
    }

}
