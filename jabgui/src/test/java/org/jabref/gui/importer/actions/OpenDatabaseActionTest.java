package org.jabref.gui.importer.actions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
public class OpenDatabaseActionTest {
    DialogService dialogService;
    GuiPreferences guiPreferences;
    OpenDatabaseAction openDatabaseAction;
    LibraryTabContainer libraryTabContainer;

    @BeforeEach
    void initializeOpenDatabaseAction() {
        dialogService = mock(DialogService.class);
        guiPreferences = mock(GuiPreferences.class);
        libraryTabContainer = mock(LibraryTabContainer.class);
        openDatabaseAction = spy(new OpenDatabaseAction(
                libraryTabContainer,
                guiPreferences,
                mock(AiService.class),
                dialogService,
                mock(StateManager.class),
                mock(FileUpdateMonitor.class),
                mock(BibEntryTypesManager.class),
                mock(ClipBoardManager.class),
                mock(TaskExecutor.class),
                new GitHandlerRegistry(mock(GitPreferences.class))
        ));
    }

    @Test
    void getFilesToOpenFailsToOpenPath(@TempDir Path tempDir) {
        Path path = tempDir.resolve("test-dir");

        FilePreferences filePreferences = mock(FilePreferences.class);
        FileDialogConfiguration badConfig = mock(FileDialogConfiguration.class);
        FileDialogConfiguration goodConfig = mock(FileDialogConfiguration.class);

        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(libraryTabContainer.getLibraryTabs()).thenReturn(FXCollections.emptyObservableList());
        when(openDatabaseAction.getInitialDirectory()).thenReturn(path);

        // Make it so that showFileOpenDialogAndGetMultipleFiles will throw an error when called with the bad path, but
        // not for the good path as in issue #10548
        when(dialogService.showFileOpenDialogAndGetMultipleFiles(badConfig))
                .thenAnswer(x -> {
                    throw new IllegalArgumentException();
                });
        when(dialogService.showFileOpenDialogAndGetMultipleFiles(goodConfig))
                .thenAnswer(x -> List.of());

        // Simulate a scenario where the initial directory is good
        when(openDatabaseAction.getFileDialogConfiguration(openDatabaseAction.getInitialDirectory()))
                .thenReturn(goodConfig);

        assertEquals(List.of(), openDatabaseAction.getFilesToOpen());

        // Simulate a scenario where the initial directory is bad and the user directory is good
        when(openDatabaseAction.getFileDialogConfiguration(openDatabaseAction.getInitialDirectory()))
                .thenReturn(badConfig);
        when(openDatabaseAction.getFileDialogConfiguration(Directories.getUserDirectory()))
                .thenReturn(goodConfig);

        assertThrows(IllegalArgumentException.class, () -> dialogService.showFileOpenDialogAndGetMultipleFiles(badConfig));
        assertDoesNotThrow(() -> dialogService.showFileOpenDialog(goodConfig));
        assertEquals(List.of(), openDatabaseAction.getFilesToOpen());
    }

    /// Sets up the preferences that [OpenDatabaseAction#loadDatabase] needs, with `backupDir` empty so that no
    /// backup-restore dialog interferes.
    private void mockPreferencesForLoad(Path backupDir) {
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.getBackupDirectory()).thenReturn(backupDir);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(guiPreferences.getImportFormatPreferences())
                .thenReturn(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    // [utest->req~import.library.unreadable-reported~1]
    @Test
    void loadDatabaseReportsFileThatCannotBeRead(@TempDir Path tempDir) throws Exception {
        // A directory can never be read as a library, on every platform
        Path unreadable = Files.createDirectory(tempDir.resolve("unreadable.bib"));
        mockPreferencesForLoad(tempDir.resolve("backups"));

        ParserResult result = openDatabaseAction.loadDatabase(unreadable);

        assertTrue(result.isInvalid());
        JavaFxExtension.awaitEvents();
        verify(dialogService).showErrorDialogAndWait(
                eq(Localization.lang("Open library error")),
                contains(unreadable.toString()));
    }

    // [utest->req~import.library.unreadable-reported~1]
    @Test
    void loadDatabaseReportsAbortedParse(@TempDir Path tempDir) throws Exception {
        // Unresolved merge conflict markers make the parser abort, so nothing at all is read
        Path conflicted = Files.writeString(tempDir.resolve("conflicted.bib"), """
                @article{a,
                <<<<<<< HEAD
                  title = {Ours},
                =======
                  title = {Theirs},
                >>>>>>> branch
                }
                """);
        mockPreferencesForLoad(tempDir.resolve("backups"));

        ParserResult result = openDatabaseAction.loadDatabase(conflicted);

        assertTrue(result.isInvalid());
        JavaFxExtension.awaitEvents();
        verify(dialogService).showErrorDialogAndWait(
                eq(Localization.lang("Open library error")),
                contains(conflicted.toString()));
    }

    // [utest->req~import.library.unreadable-reported~1]
    @Test
    void loadDatabaseReportsNothingForReadableFile(@TempDir Path tempDir) throws Exception {
        Path readable = Files.writeString(tempDir.resolve("readable.bib"), "@article{a, title = {Good}}\n");
        mockPreferencesForLoad(tempDir.resolve("backups"));

        ParserResult result = openDatabaseAction.loadDatabase(readable);

        assertFalse(result.isInvalid());
        assertEquals(1, result.getDatabase().getEntryCount());
        JavaFxExtension.awaitEvents();
        verify(dialogService, never()).showErrorDialogAndWait(anyString(), anyString());
    }
}
