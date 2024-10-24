package org.jabref.logic.util;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileDirectoryHandlerTest {

    private static final Path MAIN_DIR = Path.of("/main/dir");
    private static final Path LIBRARY_SPECIFIC_DIR = Path.of("/LibrarySpecific/dir");
    private static final Path USER_DIR = Path.of("/user/dir");
    private FileDirectoryHandler fileDirectoryHandler;
    private FilePreferences filePreferences;
    private BibDatabaseContext databaseContext;
    private BibDatabaseContext.FileDirectoriesInfo directoriesInfo;

    @BeforeEach
    public void setUp() {
        databaseContext = mock(BibDatabaseContext.class);
        filePreferences = mock(FilePreferences.class);
        DialogService dialogService = mock(DialogService.class);
        directoriesInfo = mock(BibDatabaseContext.FileDirectoriesInfo.class);

        when(databaseContext.getFileDirectoriesInfo(filePreferences)).thenReturn(directoriesInfo);
        when(filePreferences.getUserAndHost()).thenReturn("testUser");

        fileDirectoryHandler = new FileDirectoryHandler(databaseContext, filePreferences, dialogService);
    }

    @Test
    public void determineTargetDirectoryNoDirectories() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(Path.of(""));
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.empty());
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.empty());

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertTrue(result.isEmpty());
    }

    @Test
    public void determineTargetDirectoryOneDirectory() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.empty());
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.empty());

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals(new FileDirectoryHandler.DirectoryInfo("main file directory", MAIN_DIR, FileDirectoryHandler.DirectoryType.MAIN), result.get());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndLibrarySpecificFileInMain() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.empty());

        Path filePath = MAIN_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("library-specific file directory", LIBRARY_SPECIFIC_DIR, FileDirectoryHandler.DirectoryType.LIBRARY_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndLibrarySpecificFileInLibrarySpecific() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.empty());

        Path filePath = LIBRARY_SPECIFIC_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("main file directory", MAIN_DIR, FileDirectoryHandler.DirectoryType.MAIN), result.get());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndLibrarySpecificFileOutside() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.empty());

        Path filePath = Path.of("/other/dir/test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("library-specific file directory", LIBRARY_SPECIFIC_DIR, FileDirectoryHandler.DirectoryType.LIBRARY_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndUserFileInMain() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.empty());
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        Path filePath = MAIN_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("user-specific file directory", USER_DIR, FileDirectoryHandler.DirectoryType.USER_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndUserFileInUser() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.empty());
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        Path filePath = USER_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("main file directory", MAIN_DIR, FileDirectoryHandler.DirectoryType.MAIN), result.get());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesLibrarySpecificAndUserFileInLibrarySpecific() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(Path.of(""));
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        Path filePath = LIBRARY_SPECIFIC_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("user-specific file directory", USER_DIR, FileDirectoryHandler.DirectoryType.USER_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesLibrarySpecificAndUserFileInUser() {
        // Setup directories
        when(directoriesInfo.mainFileDirectory()).thenReturn(Path.of(""));
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        // Test file in user directory
        Path filePath = USER_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);
        assertEquals(new FileDirectoryHandler.DirectoryInfo("library-specific file directory", LIBRARY_SPECIFIC_DIR, FileDirectoryHandler.DirectoryType.LIBRARY_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryThreeDirectories() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals(new FileDirectoryHandler.DirectoryInfo("library-specific file directory", LIBRARY_SPECIFIC_DIR, FileDirectoryHandler.DirectoryType.LIBRARY_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryThreeDirectoriesFileInLibrarySpecific() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        Path filePath = LIBRARY_SPECIFIC_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("user-specific file directory", USER_DIR, FileDirectoryHandler.DirectoryType.USER_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryThreeDirectoriesFileInUser() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        Path filePath = USER_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("library-specific file directory", LIBRARY_SPECIFIC_DIR, FileDirectoryHandler.DirectoryType.LIBRARY_SPECIFIC), result.get());
    }

    @Test
    public void determineTargetDirectoryThreeDirectoriesFileOutside() {
        when(directoriesInfo.mainFileDirectory()).thenReturn(MAIN_DIR);
        when(directoriesInfo.librarySpecificDirectory()).thenReturn(Optional.of(LIBRARY_SPECIFIC_DIR));
        when(directoriesInfo.userFileDirectory()).thenReturn(Optional.of(USER_DIR));

        Path filePath = Path.of("/other/dir/test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals(new FileDirectoryHandler.DirectoryInfo("library-specific file directory", LIBRARY_SPECIFIC_DIR, FileDirectoryHandler.DirectoryType.LIBRARY_SPECIFIC), result.get());
    }
}
