package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class LinkedFileViewModelMoveFileTest {

    @TempDir Path tempDir;

    private BibDatabaseContext databaseContext;
    private GuiPreferences preferences;
    private FilePreferences filePreferences;
    private DialogService dialogService;
    private TaskExecutor taskExecutor;
    private BibEntry entry;

    private Path sourceDir;
    private Path destinationDir;

    @BeforeEach
    void setUp() throws IOException {
        databaseContext = mock(BibDatabaseContext.class);
        preferences = mock(GuiPreferences.class);
        filePreferences = mock(FilePreferences.class);
        dialogService = mock(DialogService.class);
        taskExecutor = mock(TaskExecutor.class);

        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class));
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");

        entry = new BibEntry(StandardEntryType.Article);
        when(databaseContext.getDatabase()).thenReturn(new BibDatabase());

        sourceDir = tempDir.resolve("source");
        destinationDir = tempDir.resolve("destination");
        Files.createDirectories(sourceDir);
        Files.createDirectories(destinationDir);
    }

    @Test
    void moveToDirectoryMovesFileToChosenTarget() throws IOException {
        Path sourceFile = sourceDir.resolve("nested/sub/test.pdf");
        Files.createDirectories(sourceFile.getParent());
        Files.createFile(sourceFile);

        LinkedFile linkedFile = new LinkedFile("desc", sourceFile, "pdf");
        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToDirectory(destinationDir);

        assertTrue(Files.exists(destinationDir.resolve("test.pdf")));
        assertFalse(Files.exists(sourceFile));
    }

    @Test
    void moveToDirectoryUsesConfiguredDirectoryPattern() throws IOException {
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");

        Path sourceFile = sourceDir.resolve("test.pdf");
        Files.createFile(sourceFile);

        LinkedFile linkedFile = new LinkedFile("desc", sourceFile, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToDirectory(destinationDir);

        String targetDirectoryName = FileUtil.createDirNameFromPattern(databaseContext.getDatabase(), entry, "[entrytype]");
        Path movedFile = destinationDir.resolve(targetDirectoryName).resolve("test.pdf");
        assertTrue(Files.exists(movedFile));
        assertFalse(Files.exists(sourceFile));
    }

    @Test
    void moveToDirectoryShowsErrorIfFileCannotBeResolved() {
        Path missingFile = sourceDir.resolve("missing.pdf");
        LinkedFile linkedFile = new LinkedFile("desc", missingFile, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        viewModel.moveToDirectory(destinationDir);

        verify(dialogService).showErrorDialogAndWait(
                eq(Localization.lang("File not found")),
                eq(Localization.lang("Could not find file '%0'.", linkedFile.getLink()))
        );
    }

    @Test
    void isInDirectoryReturnsTrueForCurrentCurrentDirectoryAndPattern() throws IOException {
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");
        String targetDirectoryName = FileUtil.createDirNameFromPattern(databaseContext.getDatabase(), entry, "[entrytype]");

        Path existingFile = destinationDir.resolve(targetDirectoryName).resolve("test.pdf");
        Files.createDirectories(existingFile.getParent());
        Files.createFile(existingFile);

        LinkedFile linkedFile = new LinkedFile("desc", existingFile, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        assertTrue(viewModel.isInCurrentDirectory(destinationDir));
    }

    @Test
    void isInDirectoryReturnsFalseForDifferentCurrentDirectory() throws IOException {
        Path existingFile = sourceDir.resolve("test.pdf");
        Files.createFile(existingFile);
        LinkedFile linkedFile = new LinkedFile("desc", existingFile, "pdf");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        assertFalse(viewModel.isInCurrentDirectory(destinationDir));
    }
}
