package org.jabref.model.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BibDatabaseContextTest {

    private Path currentWorkingDir;

    private FilePreferences fileDirPrefs;

    @BeforeEach
    void setUp() {
        fileDirPrefs = mock(FilePreferences.class);
        currentWorkingDir = Paths.get(System.getProperty("user.dir"));
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(true);
    }

    @Test
    void getFileDirectoriesWithEmptyDbParent() {
        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(Paths.get("biblio.bib"));
        assertEquals(Collections.singletonList(currentWorkingDir),
                database.getFileDirectoriesAsPaths(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeDbParent() {
        Path file = Paths.get("relative/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent())),
                database.getFileDirectoriesAsPaths(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeDottedDbParent() {
        Path file = Paths.get("./relative/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent())),
                database.getFileDirectoriesAsPaths(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithAbsoluteDbParent() {
        Path file = Paths.get("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent())),
                database.getFileDirectoriesAsPaths(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeMetadata() {
        Path file = Paths.get("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        database.getMetaData().setDefaultFileDirectory("../Literature");
        assertEquals(Arrays.asList(currentWorkingDir.resolve(file.getParent()), Paths.get("/absolute/Literature").toAbsolutePath()),
                database.getFileDirectoriesAsPaths(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithMetadata() {
        Path file = Paths.get("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        database.getMetaData().setDefaultFileDirectory("Literature");
        assertEquals(Arrays.asList(currentWorkingDir.resolve(file.getParent()), Paths.get("/absolute/subdir/Literature").toAbsolutePath()),
                database.getFileDirectoriesAsPaths(fileDirPrefs));
    }

    @Test
    void testTypeBasedOnDefaultBiblatex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBLATEX);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }

    @Test
    void testTypeBasedOnDefaultBibtex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        assertEquals(BibDatabaseMode.BIBTEX, bibDatabaseContext.getMode());
    }

    @Test
    void testTypeBasedOnInferredModeBiblatex() {
        BibDatabase db = new BibDatabase();
        BibEntry e1 = new BibEntry(IEEETranEntryType.Electronic);
        db.insertEntry(e1);

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(db);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }
}
