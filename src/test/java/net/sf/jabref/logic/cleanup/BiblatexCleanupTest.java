package net.sf.jabref.logic.cleanup;

import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BiblatexCleanupTest {

    @Rule
    public TemporaryFolder bibFolder = new TemporaryFolder();

    private BiblatexCleanup worker;

    @Before
    public void setUp() {
        worker = new BiblatexCleanup();
    }

    @Test
    public void convertToBiblatexMovesYearMonthToDate() {
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.YEAR, "2011");
        entry.setField(FieldName.MONTH, "#jan#");

        worker.cleanup(entry);
        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.MONTH));
        Assert.assertEquals(Optional.of("2011-01"), entry.getField(FieldName.DATE));
    }

    @Test
    public void convertToBiblatexDateAlreadyPresent() {
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.YEAR, "2011");
        entry.setField(FieldName.MONTH, "#jan#");
        entry.setField(FieldName.DATE, "2012");

        worker.cleanup(entry);
        Assert.assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.of("#jan#"), entry.getField(FieldName.MONTH));
        Assert.assertEquals(Optional.of("2012"), entry.getField(FieldName.DATE));
    }
}
