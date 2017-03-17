package org.jabref.logic.cleanup;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BiblatexCleanupTest {

    private BiblatexCleanup worker;

    @Before
    public void setUp() {
        worker = new BiblatexCleanup();
    }

    @Test
    public void convertToBiblatexMovesYearMonthToDate() {
        BibEntry entry = new BibEntry();
        entry.setField("year", "2011");
        entry.setField("month", "#jan#");

        worker.cleanup(entry);
        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.MONTH));
        Assert.assertEquals(Optional.of("2011-01"), entry.getField(FieldName.DATE));
    }

    @Test
    public void convertToBiblatexDateAlreadyPresent() {
        BibEntry entry = new BibEntry();
        entry.setField("year", "2011");
        entry.setField("month", "#jan#");
        entry.setField("date", "2012");

        worker.cleanup(entry);
        Assert.assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.of("#jan#"), entry.getField(FieldName.MONTH));
        Assert.assertEquals(Optional.of("2012"), entry.getField(FieldName.DATE));
    }
}
