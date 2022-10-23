package org.jabref.logic.mergelibraries;

import com.google.common.collect.ImmutableList;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.mergeLibraries.MergeCommand;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.gui.Globals.entryTypesManager;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.mockito.Answers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public class MergeCommandTest {
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private PreferencesService preferencesService = mock(PreferencesService.class);
    private StateManager stateManager = mock(StateManager.class);

    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();


    @BeforeEach
    void setUp() {
        when(jabRefFrame.getDialogService()).thenReturn(mock(DialogService.class));
        when(preferencesService.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferencesService.getFilePreferences().getUser()).thenReturn("MockedUser");
        when(preferencesService.getImporterPreferences()).thenReturn(mock(ImporterPreferences.class));
        when(preferencesService.getImportFormatPreferences()).thenReturn(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

    }

    /**
     *  Provides a simple test to ensure that two files in the same directory
     *  can be merged
     * @throws IOException
     */
    @Test
    public void simpleMergeTest() throws IOException {
        MergeCommand command = new MergeCommand(jabRefFrame, preferencesService, stateManager);
        BibDatabase db = new BibDatabase();

        // For a simple file system with Unix-style paths and behavior:
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path foo = fs.getPath("/foo");
        Files.createDirectory(foo);

        Path a = foo.resolve("a.bib"); // /foo/hello.txt
        Files.write(a, ImmutableList.of(
                        "@misc{Leon2009,\n" +
                        "  author    = {Leon},\n" +
                        "  editor    = {Qingxuan},\n" +
                        "  publisher = {Liu inc.},\n" +
                        "  title     = {Crazy Mind},\n" +
                        "  year      = {2009},\n" +
                        "}\n" +
                        "\n" +
                        "@Comment{jabref-meta: databaseType:bibtex;}"), StandardCharsets.UTF_8);

        Path b = foo.resolve("b.bib"); // /foo/hello.txt
        Files.write(b, ImmutableList.of(
                        "@misc{Brown2001,\n" +
                        "  author    = {Jimmy Brown},\n" +
                        "  editor    = {Brian Smith},\n" +
                        "  publisher = {Liu inc.},\n" +
                        "  title     = {Jimmy's Normal Adventures},\n" +
                        "  year      = {2001},\n" +
                        "}\n" +
                        "\n" +
                        "@Comment{jabref-meta: databaseType:bibtex;}"), StandardCharsets.UTF_8);


        command.doMerge(foo, db);

        BibDatabase testDB = new BibDatabase();



        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE,"Jimmy's Normal Adventures");
        entry.setField(StandardField.AUTHOR,"Jimmy Brown");
        entry.setField(StandardField.EDITOR,"Brian Smith");
        entry.setField(StandardField.PUBLISHER,"Smith inc.");
        entry.setField(StandardField.YEAR,"2001");
        entry.setField(StandardField.KEY,"Brown2001");
        testDB.insertEntry(entry);

        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.TITLE,"Crazy Mind");
        entry2.setField(StandardField.AUTHOR,"Leon");
        entry2.setField(StandardField.EDITOR,"Qingxuan");
        entry2.setField(StandardField.PUBLISHER,"Liu inc.");
        entry2.setField(StandardField.YEAR,"2009");
        entry2.setField(StandardField.KEY,"Leon2009");
        testDB.insertEntry(entry2);

        for (BibEntry dbEntry : db.getEntries()) {
            boolean equalFlag = false;
            System.out.println("dbEntry: " + dbEntry);
            for (BibEntry testDbEntry : testDB.getEntries()) {
                System.out.println("---- testDbEntry: " + testDbEntry);
                if (new DuplicateCheck(entryTypesManager).isDuplicate(testDbEntry, dbEntry, BibDatabaseMode.BIBTEX)) {
                    equalFlag = true;
                    break;
                }
            }
            assertTrue(equalFlag);
        }
    }

    /**
     *  Provides a test to ensure that libraries in different files can be merged
     * @throws IOException
     */
    @Test
    public void multipleDirectoryMergeTest() throws IOException {
        MergeCommand command = new MergeCommand(jabRefFrame, preferencesService, stateManager);
        BibDatabase db = new BibDatabase();

        // For a simple file system with Unix-style paths and behavior:
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path foo = fs.getPath("/foo");
        Files.createDirectory(foo);
        Path bar = fs.getPath("/foo/bar");
        Files.createDirectory(bar);

        Path a = foo.resolve("a.bib"); // /foo/hello.txt
        Files.write(a, ImmutableList.of(
                "@misc{Leon2009,\n" +
                        "  author    = {Leon},\n" +
                        "  editor    = {Qingxuan},\n" +
                        "  publisher = {Liu inc.},\n" +
                        "  title     = {Crazy Mind},\n" +
                        "  year      = {2009},\n" +
                        "}\n" +
                        "\n" +
                        "@Comment{jabref-meta: databaseType:bibtex;}"), StandardCharsets.UTF_8);

        Path b = bar.resolve("b.bib"); // /foo/hello.txt
        Files.write(b, ImmutableList.of(
                "@misc{Brown2001,\n" +
                        "  author    = {Jimmy Brown},\n" +
                        "  editor    = {Brian Smith},\n" +
                        "  publisher = {Liu inc.},\n" +
                        "  title     = {Jimmy's Normal Adventures},\n" +
                        "  year      = {2001},\n" +
                        "}\n" +
                        "\n" +
                        "@Comment{jabref-meta: databaseType:bibtex;}"), StandardCharsets.UTF_8);


        command.doMerge(foo, db);

        BibDatabase testDB = new BibDatabase();



        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE,"Jimmy's Normal Adventures");
        entry.setField(StandardField.AUTHOR,"Jimmy Brown");
        entry.setField(StandardField.EDITOR,"Brian Smith");
        entry.setField(StandardField.PUBLISHER,"Smith inc.");
        entry.setField(StandardField.YEAR,"2001");
        entry.setField(StandardField.KEY,"Brown2001");
        testDB.insertEntry(entry);

        BibEntry entry2 = new BibEntry();
        entry2.setField(StandardField.TITLE,"Crazy Mind");
        entry2.setField(StandardField.AUTHOR,"Leon");
        entry2.setField(StandardField.EDITOR,"Qingxuan");
        entry2.setField(StandardField.PUBLISHER,"Liu inc.");
        entry2.setField(StandardField.YEAR,"2009");
        entry2.setField(StandardField.KEY,"Leon2009");
        testDB.insertEntry(entry2);

        for (BibEntry dbEntry : db.getEntries()) {
            boolean equalFlag = false;
            System.out.println("dbEntry: " + dbEntry);
            for (BibEntry testDbEntry : testDB.getEntries()) {
                System.out.println("---- testDbEntry: " + testDbEntry);
                if (new DuplicateCheck(entryTypesManager).isDuplicate(testDbEntry, dbEntry, BibDatabaseMode.BIBTEX)) {
                    equalFlag = true;
                    break;
                }
            }
            assertTrue(equalFlag);
        }
    }

    /**
     *  Provides a simple test to ensure that two equal entries are not added
     * @throws IOException
     */
    @Test
    public void duplicateTest() throws IOException {
        MergeCommand command = new MergeCommand(jabRefFrame, preferencesService, stateManager);
        BibDatabase db = new BibDatabase();

        // For a simple file system with Unix-style paths and behavior:
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path foo = fs.getPath("/foo");
        Files.createDirectory(foo);

        Path a = foo.resolve("a.bib"); // /foo/hello.txt
        Files.write(a, ImmutableList.of(
                "@misc{Brown2001,\n" +
                        "  author    = {Jimmy Brown},\n" +
                        "  editor    = {Brian Smith},\n" +
                        "  publisher = {Liu inc.},\n" +
                        "  title     = {Jimmy's Normal Adventures},\n" +
                        "  year      = {2001},\n" +
                        "}\n" +
                        "\n" +
                        "@Comment{jabref-meta: databaseType:bibtex;}"), StandardCharsets.UTF_8);

        Path b = foo.resolve("b.bib"); // /foo/hello.txt
        Files.write(b, ImmutableList.of(
                "@misc{Brown2001,\n" +
                        "  author    = {Jimmy Brown},\n" +
                        "  editor    = {Brian Smith},\n" +
                        "  publisher = {Liu inc.},\n" +
                        "  title     = {Jimmy's Normal Adventures},\n" +
                        "  year      = {2001},\n" +
                        "}\n" +
                        "\n" +
                        "@Comment{jabref-meta: databaseType:bibtex;}"), StandardCharsets.UTF_8);


        command.doMerge(foo, db);

        BibDatabase testDB = new BibDatabase();



        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE,"Jimmy's Normal Adventures");
        entry.setField(StandardField.AUTHOR,"Jimmy Brown");
        entry.setField(StandardField.EDITOR,"Brian Smith");
        entry.setField(StandardField.PUBLISHER,"Smith inc.");
        entry.setField(StandardField.YEAR,"2001");
        entry.setField(StandardField.KEY,"Brown2001");
        testDB.insertEntry(entry);

        for (BibEntry dbEntry : db.getEntries()) {
            boolean equalFlag = false;
            System.out.println("dbEntry: " + dbEntry);
            for (BibEntry testDbEntry : testDB.getEntries()) {
                System.out.println("---- testDbEntry: " + testDbEntry);
                if (new DuplicateCheck(entryTypesManager).isDuplicate(testDbEntry, dbEntry, BibDatabaseMode.BIBTEX)) {
                    equalFlag = true;
                    break;
                }
            }
            assertTrue(equalFlag);
        }
    }

}
