package net.sf.jabref.logic.cleanup;

import java.io.IOException;
import java.util.Optional;

import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.mockito.Mockito.mock;

public class BiblatexCleanupTest {

    @Rule
    public TemporaryFolder bibFolder = new TemporaryFolder();

    private CleanupWorker worker;

    @Before
    public void setUp() throws IOException {

        MetaData metaData = new MetaData();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, bibFolder.newFile("test.bib"));
        worker = new CleanupWorker(context,
                new CleanupPreferences(JabRefPreferences.getInstance().get(JabRefPreferences.IMPORT_FILENAMEPATTERN),
                        mock(LayoutFormatterPreferences.class),
                        JabRefPreferences.getInstance().getFileDirectoryPreferences()));
    }

    @Test
    public void convertToBiblatexMovesJournalToJournalTitle() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.JOURNAL, "test");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.JOURNAL));
        Assert.assertEquals(Optional.of("test"), entry.getField(FieldName.JOURNALTITLE));
    }

    @Test
    public void convertToBiblatexMovesYearMonthToDate() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.YEAR, "2011");
        entry.setField(FieldName.MONTH, "#jan#");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.empty(), entry.getField(FieldName.MONTH));
        Assert.assertEquals(Optional.of("2011-01"), entry.getField(FieldName.DATE));
    }

    @Test
    public void convertToBiblatexDateAlreadyPresent() {
        CleanupPreset preset = new CleanupPreset(CleanupPreset.CleanupStep.CONVERT_TO_BIBLATEX);
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.YEAR, "2011");
        entry.setField(FieldName.MONTH, "#jan#");
        entry.setField(FieldName.DATE, "2012");

        worker.cleanup(preset, entry);
        Assert.assertEquals(Optional.of("2011"), entry.getField(FieldName.YEAR));
        Assert.assertEquals(Optional.of("#jan#"), entry.getField(FieldName.MONTH));
        Assert.assertEquals(Optional.of("2012"), entry.getField(FieldName.DATE));

    }
}
