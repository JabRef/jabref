package org.jabref.logic.mergelibraries;

import com.google.common.collect.ImmutableList;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.mergeLibraries.MergeCommand;
import org.jabref.logic.importer.fileformat.bibtexml.Entry;
import org.jabref.logic.importer.fileformat.endnote.Database;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import scala.sys.process.ProcessBuilderImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;


public class MergeCommandTest {
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private PreferencesService preferencesService = mock(PreferencesService.class);
    private StateManager stateManager = mock(StateManager.class);

    @Test
    public void testDoMerge() throws IOException {
        MergeCommand command = new MergeCommand(jabRefFrame, preferencesService, stateManager);
        BibDatabase db = new BibDatabase();

        // For a simple file system with Unix-style paths and behavior:
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path foo = fs.getPath("/foo");

        Files.createDirectory(foo);

        Path a = foo.resolve("a.bib"); // /foo/hello.txt
        Files.write(a, ImmutableList.of(
                        "@Book{Leon2009,\n" +
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
                        "@Book{Brown2001,\n" +
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

        assertEquals(db, testDB);
    }

}
