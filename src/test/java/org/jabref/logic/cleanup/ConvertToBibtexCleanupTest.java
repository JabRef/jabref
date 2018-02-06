package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConvertToBibtexCleanupTest {

    private ConvertToBibtexCleanup worker;

    @Before
    public void setUp() {
        worker = new ConvertToBibtexCleanup();
    }

    @Test
    public void cleanupMovesDateToYearAndMonth() {
        BibEntry entry = new BibEntry().withField("date", "2011-01");

        worker.cleanup(entry);

        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.DATE));
        Assert.assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.of("#jan#"), entry.getField(FieldName.MONTH));
    }

    @Test
    public void cleanupWithYearAlreadyPresentDoesNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("year", "2011");
        entry.setField("date", "2012");

        worker.cleanup(entry);

        Assert.assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.of("2012"), entry.getField(FieldName.DATE));
    }

    @Test
    public void cleanupMovesJournaltitleToJournal() {
        BibEntry entry = new BibEntry().withField("journaltitle", "Best of JabRef");

        worker.cleanup(entry);

        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.JOURNALTITLE));
        Assert.assertEquals(Optional.of("Best of JabRef"), entry.getField(FieldName.JOURNAL));
    }
}
