package net.sf.jabref.logic.cleanup;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.journals.JournalAbbreviationRepository;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

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

        RenamePdfCleanup cleanup = new RenamePdfCleanup(
                Collections.singletonList(testFolder.getRoot().getAbsolutePath()), false,
                null, mock(JournalAbbreviationRepository.class));

        File tempFile = testFolder.newFile("toot.tmp");
        BibEntry entry = new BibEntry();
        entry.setField(BibEntry.KEY_FIELD, "Toot");
        FileField.ParsedFileField fileField = new FileField.ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        cleanup.cleanup(entry);
        FileField.ParsedFileField newFileField = new FileField.ParsedFileField("", "Toot.tmp", "");
        assertEquals(FileField.getStringRepresentation(newFileField), entry.getField("file"));
    }
}