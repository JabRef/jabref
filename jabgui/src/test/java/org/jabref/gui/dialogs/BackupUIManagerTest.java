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
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import org.controlsfx.control.HyperlinkLabel;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
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
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.emptyObservableList());

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
    void resolverDialogRaisesTabOfAffectedLibraryFirst(@TempDir Path tempDir) throws IOException {
        Path backupDir = tempDir.resolve("backups");
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        when(dialogService.showCustomDialogAndWait(any(BackupResolverDialog.class)))
                .thenReturn(Optional.of(BackupResolverDialog.IGNORE_BACKUP));

        Path originalFile = tempDir.resolve("library.bib");
        Files.writeString(originalFile, "@article{original}");
        Files.writeString(BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalFile, BackupFileType.BACKUP, backupDir), "@article{backup}");

        LibraryTab affectedTab = tabFor(originalFile);
        LibraryTab otherTab = tabFor(tempDir.resolve("other.bib"));
        LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
        when(tabContainer.getLibraryTabs()).thenReturn(FXCollections.observableArrayList(otherTab, affectedTab));

        interact(() -> BackupUIManager.showRestoreBackupDialog(
                dialogService,
                tabContainer,
                originalFile,
                preferences,
                mock(FileUpdateMonitor.class),
                mock(StateManager.class)));

        InOrder inOrder = inOrder(tabContainer, dialogService);
        inOrder.verify(tabContainer).showLibraryTab(affectedTab);
        inOrder.verify(dialogService).showCustomDialogAndWait(any(BackupResolverDialog.class));
    }

    private static LibraryTab tabFor(Path file) {
        BibDatabaseContext context = new BibDatabaseContext();
        context.setDatabasePath(file);
        LibraryTab tab = mock(LibraryTab.class);
        when(tab.getBibDatabaseContext()).thenReturn(context);
        return tab;
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
}
