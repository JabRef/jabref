package org.jabref.model.database;

import java.nio.file.Path;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertEquals(List.of(currentWorkingDir), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeDbParent() {
        Path file = Path.of("relative/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(List.of(currentWorkingDir.resolve(file.getParent())), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeDottedDbParent() {
        Path file = Path.of("./relative/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(List.of(currentWorkingDir.resolve(file.getParent())), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithDotAsDirectory() {
        Path file = Path.of("biblio.bib");
        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(currentWorkingDir.resolve(file));
        database.getMetaData().setDefaultFileDirectory(".");
        assertEquals(List.of(currentWorkingDir), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithAbsoluteDbParent() {
        Path file = Path.of("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        assertEquals(List.of(currentWorkingDir.resolve(file.getParent())), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithRelativeMetadata() {
        Path file = Path.of("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        database.getMetaData().setDefaultFileDirectory("../Literature");
        assertEquals(List.of(
                        // first directory originates from the metadata
                        Path.of("/absolute/Literature").toAbsolutePath(),
                        Path.of("/absolute/subdir").toAbsolutePath()
                ),
                database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getFileDirectoriesWithMetadata() {
        Path file = Path.of("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(file);
        database.getMetaData().setDefaultFileDirectory("Literature");
        assertEquals(List.of(
                        // first directory originates from the metadata
                        Path.of("/absolute/subdir/Literature").toAbsolutePath(),
                        Path.of("/absolute/subdir").toAbsolutePath()
                ),
                database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void getUserFileDirectoryIfAllAreEmpty() {
        when(fileDirPrefs.shouldStoreFilesRelativeToBibFile()).thenReturn(false);
        Path userDirJabRef = OS.getNativeDesktop().getDefaultFileChooserDirectory();

        when(fileDirPrefs.getMainFileDirectory()).thenReturn(Optional.of(userDirJabRef));
        BibDatabaseContext database = new BibDatabaseContext();
        database.setDatabasePath(Path.of("biblio.bib"));
        assertEquals(List.of(userDirJabRef), database.getFileDirectories(fileDirPrefs));
    }

    @Test
    void typeBasedOnDefaultBiblatex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBLATEX);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }

    @Test
    void typeBasedOnDefaultBibtex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        assertEquals(BibDatabaseMode.BIBTEX, bibDatabaseContext.getMode());
    }

    @Test
    void typeBasedOnInferredModeBiblatex() {
        BibDatabase db = new BibDatabase();
        BibEntry e1 = new BibEntry(IEEETranEntryType.Electronic);
        db.insertEntry(e1);

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(db);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }

    @Test
    void getFullTextIndexPathWhenPathIsNull() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setDatabasePath(null);

        Path expectedPath = OS.getNativeDesktop().getFulltextIndexBaseDirectory().resolve("unsaved");
        Path actualPath = bibDatabaseContext.getFulltextIndexPath();

        assertEquals(expectedPath, actualPath);
    }

    @Test
    void getFullTextIndexPathWhenPathIsNotNull() {
        Path existingPath = Path.of("some_path.bib");

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setDatabasePath(existingPath);

        Path actualPath = bibDatabaseContext.getFulltextIndexPath();
        assertNotNull(actualPath);

        String fulltextIndexBaseDirectory = OS.getNativeDesktop().getFulltextIndexBaseDirectory().toString();
        String actualPathStart = actualPath.toString().substring(0, fulltextIndexBaseDirectory.length());
        assertEquals(fulltextIndexBaseDirectory, actualPathStart);
    }
}
