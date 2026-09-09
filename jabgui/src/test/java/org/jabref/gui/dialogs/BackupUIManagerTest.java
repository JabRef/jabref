package org.jabref.gui.dialogs;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.undo.GuiUndoManager;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.controlsfx.control.HyperlinkLabel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class BackupUIManagerTest extends JavaFxTest {

    private DialogService dialogService;
    private GuiPreferences preferences;

    @BeforeEach
    void setUp() {
        Localization.setLanguage(Language.ENGLISH);
        dialogService = mock(DialogService.class);
        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class));

        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(BibEntryTypesManager.class, new BibEntryTypesManager());
        Injector.setModelOrService(TaskExecutor.class, new CurrentThreadTaskExecutor());
        Injector.setModelOrService(KeyBindingRepository.class, mock(KeyBindingRepository.class));
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
    }

    @Test
    void failedRestoreShowsBackupPathAndCause(@TempDir Path tempDir) throws IOException {
        Path backupDir = tempDir.resolve("backups");
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        when(dialogService.showCustomDialogAndWait(any(BackupResolverDialog.class)))
                .thenReturn(Optional.of(BackupResolverDialog.RESTORE_FROM_BACKUP));

        Path originalFile = tempDir.resolve("library.bib");
        Files.createDirectory(originalFile);
        Files.writeString(originalFile.resolve("existing-file"), "existing content");
        Path backupFile = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalFile, BackupFileType.BACKUP, backupDir);
        Files.writeString(backupFile, "@article{backup}");

        LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.observableArrayList());

        interact(() -> BackupUIManager.showRestoreBackupDialog(
                dialogService,
                tabContainer,
                originalFile,
                preferences,
                mock(FileUpdateMonitor.class),
                mock(StateManager.class)));

        verify(dialogService).showErrorDialogAndWait(
                eq(Localization.lang("Restore backup")),
                eq(Localization.lang("Could not restore the backup file '%0'.", backupFile)),
                any(DirectoryNotEmptyException.class));
    }

    @Test
    void backupResolverDialogShowsLibraryAndBackupSizes(@TempDir Path tempDir) throws IOException {
        Path originalFile = tempDir.resolve("library.bib");
        Files.write(originalFile, new byte[1024]);
        Path backupFile = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalFile, BackupFileType.BACKUP, tempDir.resolve("backups"));
        Files.write(backupFile, new byte[2048]);

        AtomicReference<@Nullable String> dialogContent = new AtomicReference<>();
        interact(() -> {
            BackupResolverDialog dialog = new BackupResolverDialog(originalFile, backupFile.getParent(), mock(ExternalApplicationsPreferences.class));
            StackPane content = (StackPane) dialog.getDialogPane().getContent();
            HyperlinkLabel hyperlink = (HyperlinkLabel) content.getChildren().getFirst();
            dialogContent.set(hyperlink.getText());
        });

        assertEquals("""
                A backup file for 'library.bib' was found at [%s]
                Current library size: 1 KB
                Backup size: 2 KB
                This could indicate that JabRef did not shut down cleanly last time the file was used.

                Do you want to recover the library from the backup file?""".formatted(backupFile.getFileName()), dialogContent.get());
    }

    @Test
    void showRestoreBackupDialogFocusesAssociatedLibraryTab(@TempDir Path tempDir) {
        // [utest->req~jabgui.autosaveandbackup.focus-backup-library-tab~1]
        Path backupDir = tempDir.resolve("backups");
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        when(dialogService.showCustomDialogAndWait(any(BackupResolverDialog.class)))
                .thenReturn(Optional.of(BackupResolverDialog.IGNORE_BACKUP));

        Path originalFile = tempDir.resolve("library.bib");

        LibraryTab targetTab = mock(LibraryTab.class);
        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(targetTab.getBibDatabaseContext()).thenReturn(context);
        when(context.getDatabasePath()).thenReturn(Optional.of(originalFile));

        LibraryTab otherTab = mock(LibraryTab.class);
        BibDatabaseContext otherContext = mock(BibDatabaseContext.class);
        when(otherTab.getBibDatabaseContext()).thenReturn(otherContext);
        when(otherContext.getDatabasePath()).thenReturn(Optional.of(tempDir.resolve("other.bib")));

        LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.observableArrayList(otherTab, targetTab));

        interact(() -> BackupUIManager.showRestoreBackupDialog(
                dialogService,
                tabContainer,
                originalFile,
                preferences,
                mock(FileUpdateMonitor.class),
                mock(StateManager.class)));

        verify(tabContainer).showLibraryTab(targetTab);
    }

    @Test
    void showReviewBackupDialogResetsChangeMonitorOnlyOnTargetTab(@TempDir Path tempDir) throws IOException {
        // [utest->req~jabgui.autosaveandbackup.focus-backup-library-tab~1]
        Path backupDir = tempDir.resolve("backups");
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);

        org.jabref.logic.preview.TextBasedPreviewLayout layout = new org.jabref.logic.preview.TextBasedPreviewLayout(mock(org.jabref.logic.layout.Layout.class));
        PreviewPreferences previewPreferences = new PreviewPreferences(
                java.util.List.of(layout),
                0,
                "",
                false,
                false,
                java.util.List.of(),
                false);
        when(preferences.getPreviewPreferences()).thenReturn(previewPreferences);

        Path originalFile = tempDir.resolve("library.bib");
        Files.writeString(originalFile, "@article{test, title = {Original}}");
        Path backupFile = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalFile, BackupFileType.BACKUP, backupDir);
        Files.writeString(backupFile, "@article{test, title = {Backup}}");

        when(dialogService.showCustomDialogAndWait(any(BackupResolverDialog.class)))
                .thenReturn(Optional.of(BackupResolverDialog.REVIEW_BACKUP));
        when(dialogService.showCustomDialogAndWait(any(DatabaseChangesResolverDialog.class)))
                .thenAnswer(invocation -> {
                    DatabaseChangesResolverDialog dialog = invocation.getArgument(0);
                    dialog.denyChanges();
                    return Optional.of(true);
                });

        LibraryTab targetTab = mock(LibraryTab.class);
        BibDatabaseContext context = mock(BibDatabaseContext.class);
        when(targetTab.getBibDatabaseContext()).thenReturn(context);
        when(context.getDatabasePath()).thenReturn(Optional.of(originalFile));

        LibraryTab otherTab = mock(LibraryTab.class);
        BibDatabaseContext otherContext = mock(BibDatabaseContext.class);
        when(otherTab.getBibDatabaseContext()).thenReturn(otherContext);
        when(otherContext.getDatabasePath()).thenReturn(Optional.of(tempDir.resolve("other.bib")));

        LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.observableArrayList(otherTab, targetTab));

        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getUndoManager(any())).thenReturn(mock(GuiUndoManager.class));

        interact(() -> BackupUIManager.showRestoreBackupDialog(
                dialogService,
                tabContainer,
                originalFile,
                preferences,
                mock(FileUpdateMonitor.class),
                stateManager));

        verify(tabContainer, atLeastOnce()).showLibraryTab(targetTab);
        verify(targetTab).resetChangeMonitor();
        verify(otherTab, never()).resetChangeMonitor();
    }
}
