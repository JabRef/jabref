package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RenamePdfCleanupTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private BibDatabaseContext context;
    private BibEntry entry;

    @Before
    public void setUp() throws Exception {
        Globals.prefs = mock(JabRefPreferences.class);

        MetaData metaData = new MetaData();
        context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        context.setDatabaseFile(testFolder.newFile("test.bib"));

        entry = new BibEntry();
        entry.setCiteKey("Toot");
    }

    /**
     * Test for #466
     */
    @Test
    public void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase() throws IOException {
        when(Globals.prefs.get("importFileNamePattern")).thenReturn("\\bibtexkey");
        File tempFile = testFolder.newFile("toot.tmp");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, mock(JournalAbbreviationLoader.class),
                Globals.prefs);
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot.tmp", "");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getFieldOptional("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesWithMultipleFiles() throws IOException {
        when(Globals.prefs.get("importFileNamePattern")).thenReturn("\\bibtexkey - \\title");
        File tempFile = testFolder.newFile("Toot.tmp");

        entry.setField("title", "test title");
        entry.setField("file", FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("","",""),
                new ParsedFileField("", tempFile.getAbsolutePath(), ""), new ParsedFileField("","",""))));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, mock(JournalAbbreviationLoader.class),
                Globals.prefs);
        cleanup.cleanup(entry);

        assertEquals(
                Optional.of(FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("", "", ""),
                        new ParsedFileField("", "Toot - test title.tmp", ""), new ParsedFileField("", "", "")))),
                entry.getFieldOptional("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileStartingWithBibtexKey() throws IOException {
        when(Globals.prefs.get("importFileNamePattern")).thenReturn("\\bibtexkey - \\title");
        File tempFile = testFolder.newFile("Toot.tmp");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, mock(JournalAbbreviationLoader.class),
                Globals.prefs);
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title.tmp", "");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getFieldOptional("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileInSameFolder() throws IOException {
        when(Globals.prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn("\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}");
        testFolder.newFile("Toot.pdf");
        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, mock(JournalAbbreviationLoader.class),
                Globals.prefs);
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getFieldOptional("file"));
    }
}
