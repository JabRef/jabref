package org.jabref.gui.fieldeditors;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileDirectoryHandlerTest {

    private static final Path MAIN_DIR = Path.of("/main/dir");
    private static final Path GENERAL_DIR = Path.of("/general/dir");
    private static final Path USER_DIR = Path.of("/user/dir");
    private FileDirectoryHandler fileDirectoryHandler;
    private FilePreferences filePreferences;
    private MetaData metaData;

    @BeforeEach
    public void setUp() {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        filePreferences = mock(FilePreferences.class);
        DialogService dialogService = mock(DialogService.class);
        metaData = mock(MetaData.class);

        when(databaseContext.getMetaData()).thenReturn(metaData);
        when(filePreferences.getUserAndHost()).thenReturn("testUser");

        fileDirectoryHandler = new FileDirectoryHandler(databaseContext, filePreferences, dialogService);
    }

    @Test
    public void determineTargetDirectoryNoDirectories() {
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void determineTargetDirectoryOneDirectory() {
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(Path.of("main/dir")));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals("main file directory", result.get().label());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndGeneralFileInMain() {
        // Setup directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        // Test file in main directory
        Path filePath = MAIN_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("library-specific file directory", result.get().label());
        assertEquals(GENERAL_DIR, result.get().path());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndGeneralFileInGeneral() {
        // Setup directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        // Test file in general directory
        Path filePath = GENERAL_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("main file directory", result.get().label());
        assertEquals(MAIN_DIR, result.get().path());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndGeneralFileOutside() {
        // Setup directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        // Test file outside both directories
        Path filePath = Path.of("/other/dir/test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("library-specific file directory", result.get().label());
        assertEquals(GENERAL_DIR, result.get().path());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndUserFileInMain() {
        // Setup directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of(USER_DIR.toString()));

        // Test file in main directory
        Path filePath = MAIN_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("user-specific file directory", result.get().label());
        assertEquals(USER_DIR, result.get().path());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesMainAndUserFileInUser() {
        // Setup directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of(USER_DIR.toString()));

        // Test file in user directory
        Path filePath = USER_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("main file directory", result.get().label());
        assertEquals(MAIN_DIR, result.get().path());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesGeneralAndUserFileInGeneral() {
        // Setup directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of(USER_DIR.toString()));

        // Test file in general directory
        Path filePath = GENERAL_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);
        assertTrue(result.isEmpty());
    }

    @Test
    public void determineTargetDirectoryTwoDirectoriesGeneralAndUserFileInUser() {
        // Setup directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of(USER_DIR.toString()));

        // Test file in user directory
        Path filePath = USER_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);
        assertTrue(result.isEmpty());
    }

    @Test
    public void determineTargetDirectoryThreeDirectories() {
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(Path.of("main/dir")));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("general/dir"));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of("user/dir"));

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals("library-specific file directory", result.get().label());
    }

    @Test
    public void determineTargetDirectoryThreeDirectoriesFileInGeneral() {
        // Setup all three directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of(USER_DIR.toString()));

        // Test file in general directory
        Path filePath = GENERAL_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("user-specific file directory", result.get().label());
        assertEquals(USER_DIR, result.get().path());
    }

    @Test
    public void determineTargetDirectoryThreeDirectoriesFileInUser() {
        // Setup all three directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of(USER_DIR.toString()));

        // Test file in user directory
        Path filePath = USER_DIR.resolve("test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("library-specific file directory", result.get().label());
        assertEquals(GENERAL_DIR, result.get().path());
    }

    @Test
    public void determineTargetDirectoryThreeDirectoriesFileOutside() {
        // Setup all three directories
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(MAIN_DIR));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of(GENERAL_DIR.toString()));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of(USER_DIR.toString()));

        // Test file outside all directories
        Path filePath = Path.of("/other/dir/test.pdf");
        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(filePath);

        assertEquals("library-specific file directory", result.get().label());
        assertEquals(GENERAL_DIR, result.get().path());
    }
}
