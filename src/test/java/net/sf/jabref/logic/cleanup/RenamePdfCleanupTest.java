package net.sf.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class RenamePdfCleanupTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private BibDatabaseContext context;
    private BibEntry entry;
    private JabRefPreferences prefs;


    @Before
    public void setUp() throws Exception {
        prefs = JabRefPreferences.getInstance();
        MetaData metaData = new MetaData();
        context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        context.setDatabaseFile(testFolder.newFile("test.bib"));

        prefs.putBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR, true);

        entry = new BibEntry();
        entry.setCiteKey("Toot");
    }

    /**
     * Test for #466
     */
    @Test
    public void cleanupRenamePdfRenamesFileEvenIfOnlyDifferenceIsCase() throws IOException {
        String fileNamePattern = "\\bibtexkey";
        String fileDirPattern = "";
        File tempFile = testFolder.newFile("toot.tmp");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                mock(LayoutFormatterPreferences.class), prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot.tmp", "");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesWithMultipleFiles() throws IOException {
        String fileNamePattern = "\\bibtexkey - \\title";
        String fileDirPattern = "";
        File tempFile = testFolder.newFile("Toot.tmp");

        entry.setField("title", "test title");
        entry.setField("file", FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("", "", ""),
                new ParsedFileField("", tempFile.getAbsolutePath(), ""), new ParsedFileField("", "", ""))));

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                mock(LayoutFormatterPreferences.class), prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);

        assertEquals(
                Optional.of(FileField.getStringRepresentation(Arrays.asList(new ParsedFileField("", "", ""),
                        new ParsedFileField("", "Toot - test title.tmp", ""), new ParsedFileField("", "", "")))),
                entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileStartingWithBibtexKey() throws IOException {
        String fileNamePattern = "\\bibtexkey - \\title";
        String fileDirPattern = "";
        File tempFile = testFolder.newFile("Toot.tmp");
        ParsedFileField fileField = new ParsedFileField("", tempFile.getAbsolutePath(), "");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                mock(LayoutFormatterPreferences.class), prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title.tmp", "");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanupRenamePdfRenamesFileInSameFolder() throws IOException {
        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        String fileDirPattern = "";
        testFolder.newFile("Toot.pdf");
        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);

        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanUpRenamePdfRenameFileDirectoryPattern() throws IOException
    {

        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        String fileDirPattern = "\\EntryType";

        testFolder.newFile("Toot.pdf");
        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);
        ParsedFileField newFileField = new ParsedFileField("", "Misc\\Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanUpRenamePdfRenameFileDirectoryPatternSubDirectory() throws IOException {

        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        String fileDirPattern = "\\EntryType";

        File subFolder = testFolder.newFolder("subbfolder");
        Path file = Files.createTempFile(subFolder.toPath(), "Toot", "pdf");



        ParsedFileField fileField = new ParsedFileField("", file.toString(), "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");


        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);
        ParsedFileField newFileField = new ParsedFileField("", "Misc\\Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanUpRenamePdfRenameFileDirectoryPatternSameAsFilePattern() throws IOException {

        String fileNamePattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";
        String fileDirPattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";

        testFolder.newFile("Toot.pdf");

        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);
        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title\\Toot - test title.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @Test
    public void cleanUpRenamePdfRenameFileDirectoryPatternEmptyFileName() throws IOException {

        String fileNamePattern = "";
        String fileDirPattern = "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}";

        testFolder.newFile("Toot.pdf");

        ParsedFileField fileField = new ParsedFileField("", "Toot.pdf", "PDF");
        entry.setField("file", FileField.getStringRepresentation(fileField));
        entry.setField("title", "test title");

        RenamePdfCleanup cleanup = new RenamePdfCleanup(false, context, fileNamePattern, fileDirPattern,
                prefs.getLayoutFormatterPreferences(mock(JournalAbbreviationLoader.class)),
                prefs.getFileDirectoryPreferences());
        cleanup.cleanup(entry);
        ParsedFileField newFileField = new ParsedFileField("", "Toot - test title\\Toot.pdf", "PDF");
        assertEquals(Optional.of(FileField.getStringRepresentation(newFileField)), entry.getField("file"));
    }

    @After
    public void tearDown() {
        JabRefPreferences.getInstance().putBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR, false);

    }
}
