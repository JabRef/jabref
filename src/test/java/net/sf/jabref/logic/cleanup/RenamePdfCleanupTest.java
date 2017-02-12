package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;
import net.sf.jabref.model.metadata.MetaData;
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
    private JabRefPreferences prefs;

    private FileDirectoryPreferences fileDirPrefs;

    @Before
    public void setUp() throws Exception {
        prefs = JabRefPreferences.getInstance();
        MetaData metaData = new MetaData();
        context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        context.setDatabaseFile(testFolder.newFile("test.bib"));

        fileDirPrefs = mock(FileDirectoryPreferences.class);
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(true); //Set Biblocation as Primary Directory, otherwise the tmp folders won't be cleaned up correctly

        entry = new BibEntry();
        entry.setCiteKey("Toot");
    }

    /**
     * Test for #466
     */
    @Test
    public void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase() throws IOException {
        String fileNamePattern = "\\bibtexkey";
        File tempFile = testFolder.newFile("toot.tmp");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                mock(LayoutFormatterPreferences.class), fileDirPrefs);
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot.tmp", "");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesWithMultipleFiles() throws IOException {
        String fileNamePattern = "\\bibtexkey - \\title";
        File tempFile = testFolder.newFile("Toot.tmp");

        entry.setField("title", "test title");
        entry.setField("file", FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("", "", ""),
                new ParsedFileField("", tempFile.getAbsolutePath(), ""), new ParsedFileField("", "", ""))));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                mock(LayoutFormatterPreferences.class), fileDirPrefs);
        cleanup.cleanup(entry);

        assertEquals(
                Optional.of(FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("", "", ""),
                        new ParsedFileField("", "Toot - test title.tmp", ""), new ParsedFileField("", "", "")))),
                entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileStartingWithBibtexKey() throws IOException {
        String fileNamePattern = "\\bibtexkey - \\title";

        File tempFile = testFolder.newFile("Toot.tmp");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                mock(LayoutFormatterPreferences.class), fileDirPrefs);
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title.tmp", "");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileInSameFolder() throws IOException {
        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        testFolder.newFile("Toot.pdf");
        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                fileDirPrefs);
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupSingleField() throws IOException {
        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        testFolder.newFile("Toot.pdf");
        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");
        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                fileDirPrefs, fileField);

        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));

    }

    @Test
    public void cleanupGetTargetFilename() throws IOException {
        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        testFolder.newFile("Toot.pdf");
        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                fileDirPrefs);
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        assertEquals("Toot - test title.pdf", cleanup.getTargetFileName(fileField, entry));
    }

}
