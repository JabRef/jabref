package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileDirectoryHandlerTest {

    private FileDirectoryHandler fileDirectoryHandler;
    private BibDatabaseContext databaseContext;
    private FilePreferences filePreferences;
    private DialogService dialogService;
    private MetaData metaData;

    @BeforeEach
    public void setUp() {
        databaseContext = mock(BibDatabaseContext.class);
        filePreferences = mock(FilePreferences.class);
        dialogService = mock(DialogService.class);
        metaData = mock(MetaData.class);

        when(databaseContext.getMetaData()).thenReturn(metaData);

        fileDirectoryHandler = new FileDirectoryHandler(databaseContext, filePreferences, dialogService);
    }

    @Test
    public void testDetermineTargetDirectory_NoDirectories() {
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals(Optional.empty(), result);
    }

    @Test
    public void testDetermineTargetDirectory_OneDirectory() {
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(Path.of("main/dir")));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.empty());
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals("main file directory", result.get().label());
    }

    @Test
    public void testDetermineTargetDirectory_TwoDirectories() {
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(Path.of("main/dir")));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("general/dir"));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.empty());

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals("library-specific file directory", result.get().label());
    }

    @Test
    public void testDetermineTargetDirectory_ThreeDirectories() {
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(Path.of("main/dir")));
        when(metaData.getDefaultFileDirectory()).thenReturn(Optional.of("general/dir"));
        when(metaData.getUserFileDirectory(Mockito.anyString())).thenReturn(Optional.of("user/dir"));

        Optional<FileDirectoryHandler.DirectoryInfo> result = fileDirectoryHandler.determineTargetDirectory(Path.of("some/path"));

        assertEquals("library-specific file directory", result.get().label());
    }
}
