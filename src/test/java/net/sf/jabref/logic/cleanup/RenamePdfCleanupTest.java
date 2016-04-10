package net.sf.jabref.logic.cleanup;

import net.sf.jabref.*;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RenamePdfCleanupTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    /**
     * Test for #466
     */
    @Test
    public void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase() throws IOException {
        Globals.prefs = mock(JabRefPreferences.class);
        when(Globals.prefs.get("importFileNamePattern")).thenReturn("\\bibtexkey");

        MetaData metaData = new MetaData();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        context.setDatabaseFile(testFolder.newFile("test.bib"));
        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, mock(JournalAbbreviationRepository.class));

        File tempFile = testFolder.newFile("toot.tmp");
        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "Toot");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        cleanup.cleanup(entry);
        ParsedFileField newFileField = new ParsedFileField("", "Toot.tmp", "");
        assertEquals(FileField.getStringRepresentation(newFileField), entry.getField("file"));
    }
}