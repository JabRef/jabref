package org.jabref.model.database;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AllowedToUseLogic("Needs access to OS class")
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
        // first directory is the metadata
        // the bib file location is not included, because only the library-configured directories should be searched and the fallback should be the global directory.
        assertEquals(List.of(Path.of("/absolute/Literature").toAbsolutePath()),
                database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithMetadata() {
        Path file = Path.of("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        database.getMetaData().setDefaultFileDirectory("Literature");
        // first directory is the metadata
        // the bib file location is not included, because only the library-configured directories should be searched and the fallback should be the global directory.
        assertEquals(List.of(Path.of("/absolute/subdir/Literature").toAbsolutePath()),
                database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getUserFileDirectoryIfAllAreEmpty() {
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        Path userDirJabRef = OS.getNativeDesktop().getDefaultFileChooserDirectory();

        when(fileDirPrefs.getMainFileDirectory()).thenReturn(Optional.of(userDirJabRef));
        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(Path.of("biblio.bib"));
        assertEquals(Collections.singletonList(userDirJabRef), database.getFileDirectories(fileDirPrefs));
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

        Path expectedPath = OS.getNativeDesktop().getFulltextIndexBaseDirectory().resolve("unsaved");
        Path actualPath = bibDatabaseContext.getFulltextIndexPath();

        assertEquals(expectedPath, actualPath);
    }

    @Test
    void testGetFullTextIndexPathWhenPathIsNotNull() {
        Path existingPath = Path.of("some_path.bib");

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setDatabasePath(existingPath);

        Path expectedPath = OS.getNativeDesktop().getFulltextIndexBaseDirectory().resolve(existingPath.hashCode() + "");
        Path actualPath = bibDatabaseContext.getFulltextIndexPath();

        assertEquals(expectedPath, actualPath);
    }
}
