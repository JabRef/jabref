package org.jabref.model.database;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;

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
        currentWorkingDir = Path.of(System.getProperty("user.dir"));
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
    }

    @Test
    void getFileDirectoriesWithEmptyDbParent() {
        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(Path.of("biblio.bib"));
        assertEquals(Collections.singletonList(currentWorkingDir), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeDbParent() {
        Path file = Path.of("relative/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent())), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeDottedDbParent() {
        Path file = Path.of("./relative/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent())), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithAbsoluteDbParent() {
        Path file = Path.of("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent())), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeMetadata() {
        Path file = Path.of("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        database.getMetaData().setDefaultFileDirectory("../Literature");
        assertEquals(Arrays.asList(Path.of("/absolute/Literature").toAbsolutePath(), currentWorkingDir.resolve(file.getParent())),
                database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithMetadata() {
        Path file = Path.of("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        database.getMetaData().setDefaultFileDirectory("Literature");
        assertEquals(Arrays.asList(Path.of("/absolute/subdir/Literature").toAbsolutePath(), currentWorkingDir.resolve(file.getParent())),
                database.getFileDirectories(fileDirPrefs));
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

    @Test
    void testGetFullTextIndexPathWhenPathIsNull() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setDatabasePath(null);

        Path expectedPath = BibDatabaseContext.getFulltextIndexBasePath().resolve("unsaved");
        Path actualPath = bibDatabaseContext.getFulltextIndexPath();

        assertEquals(expectedPath, actualPath);
    }

    @Test
    void testGetFullTextIndexPathWhenPathIsNotNull() {
        Path existingPath = Path.of("some_path.bib");

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setDatabasePath(existingPath);

        Path expectedPath = BibDatabaseContext.getFulltextIndexBasePath().resolve(existingPath.hashCode() + "");
        Path actualPath = bibDatabaseContext.getFulltextIndexPath();

        assertEquals(expectedPath, actualPath);
    }
}
