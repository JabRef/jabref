package org.jabref.gui.dialogs;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.backup.BackupResolverDialog;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangesResolverDialog;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.GroupTreeNode;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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

        interact(() -> BackupUIManager.showRestoreBackupDialog(
                dialogService,
                originalFile,
                preferences,
                mock(FileUpdateMonitor.class),
                mock(StateManager.class)));

        verify(dialogService).showErrorDialogAndWait(
                eq(Localization.lang("Restore backup")),
                eq(Localization.lang("Could not restore the backup file '%0'.", backupFile)),
                any(DirectoryNotEmptyException.class));
    }

    /// The review runs while the library is still being opened, so no tab exists for it yet and the active tab may be
    /// absent altogether (welcome tab, startup). The review must not depend on it. The backup adds groups to a library
    /// without groups, which produces a metadata change and a group change; accepting both has to install the groups.
    @Test
    void reviewBackupAcceptingAllChangesWorksWithoutAnActiveTab(@TempDir Path tempDir) throws IOException {
        Path backupDir = tempDir.resolve("backups");
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        when(dialogService.showCustomDialogAndWait(any(BackupResolverDialog.class)))
                .thenReturn(Optional.of(BackupResolverDialog.REVIEW_BACKUP));
        when(dialogService.showCustomDialogAndWait(any(DatabaseChangesResolverDialog.class)))
                .thenAnswer(invocation -> {
                    DatabaseChangesResolverDialog dialog = invocation.getArgument(0);
                    dialog.getResolvedChanges().forEach(DatabaseChange::accept);
                    return Optional.of(true);
                });
        // Sealed, so deep stubs cannot mock it
        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();
        doReturn(new TextBasedPreviewLayout("", mock(LayoutFormatterPreferences.class), mock(JournalAbbreviationRepository.class)))
                .when(previewPreferences).getSelectedPreviewLayout();
        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(BibEntryTypesManager.class, new BibEntryTypesManager());
        Injector.setModelOrService(TaskExecutor.class, mock(TaskExecutor.class));
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));

        Path originalFile = tempDir.resolve("library.bib");
        Files.writeString(originalFile, "@Article{original, title = {Original}}");
        Path backupFile = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalFile, BackupFileType.BACKUP, backupDir);
        Files.writeString(backupFile, """
                @Article{original,
                  title = {Original},
                }
                @Article{added,
                  title = {Added},
                }

                @Comment{jabref-meta: grouping:
                0 AllEntriesGroup:;
                1 StaticGroup:TODO\\;0\\;1\\;0x8a8a8aff\\;\\;\\;;
                }
                """);

        // Called off the JavaFX thread, as the library loading task does
        Optional<ParserResult> result = BackupUIManager.showRestoreBackupDialog(
                dialogService,
                originalFile,
                preferences,
                mock(FileUpdateMonitor.class),
                new JabRefGuiStateManager());

        BibDatabaseContext restored = result.orElseThrow().getDatabaseContext();
        assertEquals(2, restored.getDatabase().getEntryCount());
        assertEquals(List.of("TODO"), restored.getMetaData().getGroups().orElseThrow().getChildren().stream().map(GroupTreeNode::getName).toList());
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

    /// A conflict-aborted original produces an invalid, empty result. Reviewing the backup merges content into that
    /// same result, so it has to stop counting as invalid - otherwise the caller reports an open error and closes the
    /// tab right after the user recovered the library.
    // [utest->req~import.library.unreadable-reported~1]
    @Test
    void recoveredEntryMakesAnInvalidResultValid() {
        ParserResult parserResult = ParserResult.fromErrorMessage("aborted parse");
        parserResult.getDatabase().insertEntry(new BibEntry(StandardEntryType.Article).withCitationKey("recovered"));

        BackupUIManager.markRecoveredIfContentRestored(parserResult);

        assertFalse(parserResult.isInvalid());
    }

    /// Accepting a change is not on its own proof that anything came back: a backup differing only in its groups
    /// yields a metadata change whose acceptance restores nothing. Such a result must stay invalid, so that an
    /// unreadable original is still reported instead of opening as an empty library.
    // [utest->req~import.library.unreadable-reported~1]
    @Test
    void resultWithoutRestoredContentStaysInvalid() {
        ParserResult parserResult = ParserResult.fromErrorMessage("aborted parse");

        BackupUIManager.markRecoveredIfContentRestored(parserResult);

        assertTrue(parserResult.isInvalid());
    }

    // [utest->req~import.library.unreadable-reported~1]
    @Test
    void recoveredStringMakesAnInvalidResultValid() {
        ParserResult parserResult = ParserResult.fromErrorMessage("aborted parse");
        parserResult.getDatabase().addString(new BibtexString("name", "content"));

        BackupUIManager.markRecoveredIfContentRestored(parserResult);

        assertFalse(parserResult.isInvalid());
    }
}
