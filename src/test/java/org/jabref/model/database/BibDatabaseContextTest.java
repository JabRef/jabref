package org.jabref.model.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.model.metadata.FileDirectoryPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BibDatabaseContextTest {

    private Path currentWorkingDir;

    // Store the minimal preferences for the
    // BibDatabaseContext.getFileDirectories(File,
    // FileDirectoryPreferences) incocation:
    private FileDirectoryPreferences fileDirPrefs;

    @BeforeEach
    public void setUp() {
        fileDirPrefs = mock(FileDirectoryPreferences.class);
        currentWorkingDir = Paths.get(System.getProperty("user.dir"));
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(true);
    }

    @Test
    public void getFileDirectoriesWithEmptyDbParent() {
        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(Paths.get("biblio.bib").toFile());
        List<String> fileDirectories = dbContext.getFileDirectories("file", fileDirPrefs);
        assertEquals(Collections.singletonList(currentWorkingDir.toString()),
                fileDirectories);
    }

    @Test
    public void getFileDirectoriesWithRelativeDbParent() {
        Path file = Paths.get("relative/subdir").resolve("biblio.bib");

        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(file.toFile());
        List<String> fileDirectories = dbContext.getFileDirectories("file", fileDirPrefs);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent()).toString()),
                fileDirectories);
    }

    @Test
    public void getFileDirectoriesWithRelativeDottedDbParent() {
        Path file = Paths.get("./relative/subdir").resolve("biblio.bib");

        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(file.toFile());
        List<String> fileDirectories = dbContext.getFileDirectories("file", fileDirPrefs);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent()).toString()),
                fileDirectories);
    }

    @Test
    public void getFileDirectoriesWithAbsoluteDbParent() {
        Path file = Paths.get("/absolute/subdir").resolve("biblio.bib");

        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(file.toFile());
        List<String> fileDirectories = dbContext.getFileDirectories("file", fileDirPrefs);
        assertEquals(Collections.singletonList(currentWorkingDir.resolve(file.getParent()).toString()),
                fileDirectories);
    }
}
