package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class LinkedFileViewModelFileRotationTest {

    @TempDir Path tempDir;

    private BibDatabaseContext databaseContext;
    private GuiPreferences preferences;
    private DialogService dialogService;
    private TaskExecutor taskExecutor;
    private BibEntry entry;

    private Path userDir;
    private Path libDir;
    private Path bibDir;

    @BeforeEach
    void setUp() throws IOException {
        databaseContext = mock(BibDatabaseContext.class);
        preferences = mock(GuiPreferences.class);
        FilePreferences filePreferences = mock(FilePreferences.class);
        dialogService = mock(DialogService.class);
        taskExecutor = mock(TaskExecutor.class);

        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class));
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");

        entry = new BibEntry();
        when(databaseContext.getDatabase()).thenReturn(new BibDatabase());

        userDir = tempDir.resolve("user");
        libDir = tempDir.resolve("lib");
        bibDir = tempDir.resolve("bib");
        Files.createDirectories(userDir);
        Files.createDirectories(libDir);
        Files.createDirectories(bibDir);
    }

    @Test
    void moveToNextRotatesFromUserToLibrary() throws IOException {
        List<Path> dirs = List.of(userDir, libDir, bibDir);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileInUser = userDir.resolve("test.pdf");
        Files.createFile(fileInUser);
        LinkedFile linkedFile = new LinkedFile("desc", fileInUser, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(libDir.resolve("test.pdf")), "File should have moved to Library directory");
        assertFalse(Files.exists(fileInUser), "File should be gone from User directory");
    }

    @Test
    void moveToNextRotatesFromLibraryToBib() throws IOException {
        List<Path> dirs = List.of(userDir, libDir, bibDir);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileInLib = libDir.resolve("test.pdf");
        Files.createFile(fileInLib);
        LinkedFile linkedFile = new LinkedFile("desc", fileInLib, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(bibDir.resolve("test.pdf")), "File should have moved to Bib directory");
    }

    @Test
    void moveToNextRotatesFromBibToUser() throws IOException {
        List<Path> dirs = List.of(userDir, libDir, bibDir);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileInBib = bibDir.resolve("test.pdf");
        Files.createFile(fileInBib);
        LinkedFile linkedFile = new LinkedFile("desc", fileInBib, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(userDir.resolve("test.pdf")));
    }

    @Test
    void moveToNextSkipsNullDirectory() throws IOException {
        List<Path> dirs = Arrays.asList(userDir, null, bibDir);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileInUser = userDir.resolve("test.pdf");
        Files.createFile(fileInUser);
        LinkedFile linkedFile = new LinkedFile("desc", fileInUser, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();
        assertTrue(Files.exists(bibDir.resolve("test.pdf")));
    }

    @Test
    void moveToNextShowsErrorIfNoOtherDirectoryConfigured() throws IOException {
        List<Path> dirs = Arrays.asList(userDir, null, null);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileInUser = userDir.resolve("test.pdf");
        Files.createFile(fileInUser);
        LinkedFile linkedFile = new LinkedFile("", fileInUser, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        verify(dialogService).showErrorDialogAndWait(eq("Move file"), eq("No suitable file directory found."));
        assertTrue(Files.exists(fileInUser));
    }

    @Test
    void moveToNextMovesToUserIfFileNotInAnyDirectory() throws IOException {
        List<Path> dirs = List.of(userDir, libDir, bibDir);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path randomDir = tempDir.resolve("random");
        Files.createDirectories(randomDir);
        Path fileRandom = randomDir.resolve("test.pdf");
        Files.createFile(fileRandom);

        LinkedFile linkedFile = new LinkedFile("desc", fileRandom, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();
        assertTrue(Files.exists(userDir.resolve("test.pdf")));
    }

    @Test
    void moveToNextMirrorsDirectoryStructure() throws IOException {
        List<Path> dirs = List.of(userDir, libDir, bibDir);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path nestedFileInUser = userDir.resolve("x/y/z/test.pdf");
        Files.createDirectories(nestedFileInUser);
        LinkedFile linkedFile = new LinkedFile("desc", nestedFileInUser, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(libDir.resolve("x/y/z/test.pdf")));
        assertFalse(Files.exists(userDir.resolve("x/y/z/test.pdf")));
    }

    @Test
    void moveToNextDoesNotMirrorDirectoryStructureWhenFileNotInConfiguredDirectory() throws IOException {
        List<Path> dirs = List.of(userDir, libDir, bibDir);
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path nestedFile = tempDir.resolve("x/y/z/test.pdf");
        Files.createDirectories(nestedFile);
        LinkedFile linkedFile = new LinkedFile("desc", nestedFile, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(userDir.resolve("test.pdf")));
        assertFalse(Files.exists(userDir.resolve("x/y/z/test.pdf")));
    }
}
