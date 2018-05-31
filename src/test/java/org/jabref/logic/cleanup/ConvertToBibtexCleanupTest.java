package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

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
        BibEntry entry = new BibEntry().withField("date", "2011-01");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(FieldName.DATE));
        assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        assertEquals(Optional.of("#jan#"), entry.getField(FieldName.MONTH));
    }

    @Test
    public void cleanupWithYearAlreadyPresentDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("year", "2011");
        entry.setField("date", "2012");

        worker.cleanup(entry);

        assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        assertEquals(Optional.of("2012"), entry.getField(FieldName.DATE));
    }

    @Test
    public void cleanupMovesJournaltitleToJournal() {
        BibEntry entry = new BibEntry().withField("journaltitle", "Best of JabRef");

        worker.cleanup(entry);

        assertEquals(Optional.empty(), entry.getField(FieldName.JOURNALTITLE));
        assertEquals(Optional.of("Best of JabRef"), entry.getField(FieldName.JOURNAL));
    }
}
