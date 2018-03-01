package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

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
        entry.setField("year", "2011");
        entry.setField("month", "#jan#");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(FieldName.YEAR));
        assertEquals(Optional.empty(), entry.getField(FieldName.MONTH));
        assertEquals(Optional.of("2011-01"), entry.getField(FieldName.DATE));
    }

    @Test
    public void cleanupWithDateAlreadyPresentDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("year", "2011");
        entry.setField("month", "#jan#");
        entry.setField("date", "2012");

        worker.cleanup(entry);

        assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        assertEquals(Optional.of("#jan#"), entry.getField(FieldName.MONTH));
        assertEquals(Optional.of("2012"), entry.getField(FieldName.DATE));
    }

    @Test
    public void cleanupMovesJournalToJournaltitle() {
        BibEntry entry = new BibEntry().withField("journal", "Best of JabRef");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(FieldName.JOURNAL));
        assertEquals(Optional.of("Best of JabRef"), entry.getField(FieldName.JOURNALTITLE));
    }
}
