package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.FileDirectories;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    static Stream<Arguments> rotationCases() {
        return Stream.of(
                Arguments.of("user", "lib"),
                Arguments.of("lib", "bib"),
                Arguments.of("bib", "user")
        );
    }

    @ParameterizedTest
    @MethodSource("rotationCases")
    void moveToNextRotates(String sourceDirectoryName, String targetDirectoryName) throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.of(userDir), Optional.of(libDir), Optional.of(bibDir));
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path sourceDirectory = tempDir.resolve(sourceDirectoryName);
        Path targetDirectory = tempDir.resolve(targetDirectoryName);

        Path fileInSource = sourceDirectory.resolve("test.pdf");
        Files.createFile(fileInSource);
        LinkedFile linkedFile = new LinkedFile("desc", fileInSource, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(targetDirectory.resolve("test.pdf")));
        assertFalse(Files.exists(fileInSource));
    }

    @Test
    void moveToNextSkipsNullDirectory() throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.of(userDir), Optional.empty(), Optional.of(bibDir));
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileInUser = userDir.resolve("test.pdf");
        Files.createFile(fileInUser);
        LinkedFile linkedFile = new LinkedFile("desc", fileInUser, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();
        assertTrue(Files.exists(bibDir.resolve("test.pdf")));
        assertFalse(Files.exists(fileInUser));
    }

    @Test
    void moveToNextShowsErrorIfNoOtherDirectoryConfigured() throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.of(userDir), Optional.empty(), Optional.empty());
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileInUser = userDir.resolve("test.pdf");
        Files.createFile(fileInUser);
        LinkedFile linkedFile = new LinkedFile("", fileInUser, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        verify(dialogService).showErrorDialogAndWait(
                eq(Localization.lang("No directory found")),
                eq(Localization.lang("Configure another directory to move file(s)."))
        );
        assertTrue(Files.exists(fileInUser));
    }

    @Test
    void moveToNextShowsErrorIfNoDirectoryConfigured() throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.empty(), Optional.empty(), Optional.empty());
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path file = tempDir.resolve("test.pdf");
        Files.createFile(file);
        LinkedFile linkedFile = new LinkedFile("", file, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        verify(dialogService).showErrorDialogAndWait(
                eq(Localization.lang("No directory found")),
                eq(Localization.lang("Configure a file directory to move file(s)."))
        );
        assertTrue(Files.exists(file));
    }

    @Test
    void moveToNextMovesToUserIfFileNotInAnyDirectory() throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.of(userDir), Optional.of(libDir), Optional.of(bibDir));
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path randomDir = tempDir.resolve("random");
        Files.createDirectories(randomDir);
        Path fileRandom = randomDir.resolve("test.pdf");
        Files.createFile(fileRandom);

        LinkedFile linkedFile = new LinkedFile("desc", fileRandom, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();
        assertTrue(Files.exists(userDir.resolve("test.pdf")));
        assertFalse(Files.exists(fileRandom));
    }

    @Test
    void moveToNextMirrorsDirectoryStructure() throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.of(userDir), Optional.of(libDir), Optional.of(bibDir));
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path nestedDir = userDir.resolve("x/y/z");
        Files.createDirectories(nestedDir);
        Path testFile = nestedDir.resolve("test.pdf");
        Files.createFile(testFile);
        LinkedFile linkedFile = new LinkedFile("desc", testFile, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(libDir.resolve("x/y/z/test.pdf")));
        assertFalse(Files.exists(userDir.resolve("x/y/z/test.pdf")));
    }

    @Test
    void moveToNextDoesNotMirrorDirectoryStructureWhenFileNotInConfiguredDirectory() throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.of(userDir), Optional.of(libDir), Optional.of(bibDir));
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path nestedDir = tempDir.resolve("x/y/z");
        Files.createDirectories(nestedDir);
        Path testFile = nestedDir.resolve("test.pdf");
        Files.createFile(testFile);
        LinkedFile linkedFile = new LinkedFile("desc", testFile, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToNextPossibleDirectory();

        assertTrue(Files.exists(userDir.resolve("test.pdf")));
        assertFalse(Files.exists(userDir.resolve("x/y/z/test.pdf")));
    }

    @Test
    void moveToNextFileWithoutParentDoesNotThrowNPE() throws IOException {
        FileDirectories dirs = new FileDirectories(Optional.of(userDir), Optional.of(libDir), Optional.of(bibDir));
        when(databaseContext.getAllFileDirectories(any())).thenReturn(dirs);

        Path fileWithoutParent = Path.of("test.pdf");
        Files.createFile(fileWithoutParent);

        LinkedFile linkedFile = new LinkedFile("desc", fileWithoutParent, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        assertDoesNotThrow(viewModel::moveToNextPossibleDirectory);
    }
}
