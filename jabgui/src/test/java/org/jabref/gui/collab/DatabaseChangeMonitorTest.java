package org.jabref.gui.collab;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.Notifications;
import org.jabref.gui.StateManager;
import org.jabref.gui.autosaveandbackup.BackupManager;
import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import com.dlsc.gemsfx.infocenter.NotificationGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DatabaseChangeMonitorTest {

    @Test
    void unregisterRemovesListenerFromOriginallyMonitoredPath(@TempDir Path tempDir) throws Exception {
        Path originalPath = tempDir.resolve("original.bib");
        Path newPath = tempDir.resolve("new.bib");

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(originalPath), Optional.of(newPath));

        FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);

        DatabaseChangeMonitor monitor = new DatabaseChangeMonitor(
                databaseContext,
                fileUpdateMonitor,
                mock(TaskExecutor.class),
                mock(DialogService.class),
                mock(GuiPreferences.class),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(LibraryTab.class));

        ArgumentCaptor<FileUpdateListener> listenerCaptor = ArgumentCaptor.forClass(FileUpdateListener.class);
        verify(fileUpdateMonitor).addListenerForFile(eq(originalPath), listenerCaptor.capture());

        monitor.unregister();

        verify(fileUpdateMonitor).removeListener(eq(originalPath), eq(listenerCaptor.getValue()));
    }

    private DatabaseChangeMonitor createMonitor(Path monitoredPath, FileUpdateMonitor fileUpdateMonitor, TaskExecutor taskExecutor) {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(monitoredPath));
        return new DatabaseChangeMonitor(
                databaseContext,
                fileUpdateMonitor,
                taskExecutor,
                mock(DialogService.class),
                mock(GuiPreferences.class),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(LibraryTab.class));
    }

    @Test
    void suspendChangeDetectionKeepsWatchingAndRescansOnResume(@TempDir Path tempDir) throws Exception {
        Path monitoredPath = tempDir.resolve("library.bib");
        Files.writeString(monitoredPath, "@misc{a,}");
        FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        DatabaseChangeMonitor monitor = createMonitor(monitoredPath, fileUpdateMonitor, taskExecutor);

        monitor.suspendChangeDetection();
        Files.writeString(monitoredPath, "@misc{a,}\n@misc{b,}");
        monitor.fileUpdated();

        verify(fileUpdateMonitor, never()).removeListener(eq(monitoredPath), eq(monitor));
        verifyNoInteractions(taskExecutor);

        monitor.resumeChangeDetection();

        verify(taskExecutor).execute(any());
    }

    @Test
    void multipleFileUpdatesDuringSuspensionScheduleOneScanOnResume(@TempDir Path tempDir) throws Exception {
        Path monitoredPath = tempDir.resolve("library.bib");
        Files.writeString(monitoredPath, "@misc{a,}");
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        DatabaseChangeMonitor monitor = createMonitor(monitoredPath, mock(FileUpdateMonitor.class), taskExecutor);

        monitor.suspendChangeDetection();
        Files.writeString(monitoredPath, "@misc{a,}\n@misc{b,}");
        monitor.fileUpdated();
        monitor.fileUpdated();

        verifyNoInteractions(taskExecutor);

        monitor.resumeChangeDetection();

        verify(taskExecutor).execute(any());
    }

    @Test
    void fileUpdateSchedulesScanWhenFileChanged(@TempDir Path tempDir) throws Exception {
        Path monitoredPath = tempDir.resolve("library.bib");
        Files.writeString(monitoredPath, "@misc{a,}");
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        DatabaseChangeMonitor monitor = createMonitor(monitoredPath, mock(FileUpdateMonitor.class), taskExecutor);

        Files.writeString(monitoredPath, "@misc{a,}\n@misc{b,}");
        monitor.fileUpdated();

        verify(taskExecutor).execute(any());
    }

    @Test
    void fileUpdateSkipsScanWhenFileUnchanged(@TempDir Path tempDir) throws Exception {
        Path monitoredPath = tempDir.resolve("library.bib");
        Files.writeString(monitoredPath, "@misc{a,}");
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        DatabaseChangeMonitor monitor = createMonitor(monitoredPath, mock(FileUpdateMonitor.class), taskExecutor);

        monitor.fileUpdated();
        monitor.resumeChangeDetection();

        verifyNoInteractions(taskExecutor);
    }

    @Test
    void monitorInstalledAfterBackupRestoreDoesNotScheduleExternalChangeReview(@TempDir Path tempDir) throws Exception {
        Path originalFile = tempDir.resolve("library.bib");
        Files.writeString(originalFile, "@misc{original,}");
        Path backupDirectory = tempDir.resolve("backups");
        Path backupFile = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(originalFile, BackupFileType.BACKUP, backupDirectory);
        Files.writeString(backupFile, "@misc{restored,}");

        assertEquals(new BackupManager.RestoreResult.Restored(), BackupManager.restoreBackup(originalFile, backupDirectory));

        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        DatabaseChangeMonitor monitor = createMonitor(originalFile, mock(FileUpdateMonitor.class), taskExecutor);
        monitor.fileUpdated();

        verifyNoInteractions(taskExecutor);
    }

    @Test
    void markConsistentWithDiskSuppressesScanForOwnSave(@TempDir Path tempDir) throws Exception {
        Path monitoredPath = tempDir.resolve("library.bib");
        Files.writeString(monitoredPath, "@misc{a,}");
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        DatabaseChangeMonitor monitor = createMonitor(monitoredPath, mock(FileUpdateMonitor.class), taskExecutor);

        monitor.suspendChangeDetection();
        // Simulate JabRef's own save: the file changes, then is marked consistent before the monitor resumes
        Files.writeString(monitoredPath, "@misc{a,}\n@misc{b,}");
        monitor.markConsistentWithDisk(null);
        monitor.fileUpdated();
        monitor.resumeChangeDetection();

        verifyNoInteractions(taskExecutor);
    }

    @Test
    void applyResolvedChangesAppliesMergedEntryAndMarksLibraryDirty() {
        BibEntry oldEntry = new BibEntry().withCitationKey("Key")
                                          .withField(StandardField.TITLE, "Old title");
        BibDatabase database = new BibDatabase(List.of(oldEntry));
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        BibEntry mergedEntry = new BibEntry(oldEntry).withField(StandardField.TITLE, "Merged title");
        EntryChange mergedChange = new EntryChange(oldEntry, mergedEntry, databaseContext);
        mergedChange.accept();

        UndoManager undoManager = new UndoManager();
        undoManager.markUnchanged();
        LibraryTab libraryTab = mock(LibraryTab.class);
        DatabaseChangeMonitor monitor = new DatabaseChangeMonitor(
                databaseContext,
                mock(FileUpdateMonitor.class),
                mock(TaskExecutor.class),
                mock(DialogService.class),
                mock(GuiPreferences.class),
                undoManager,
                mock(StateManager.class),
                libraryTab);

        monitor.applyResolvedChanges(List.of(mergedChange), false);

        assertEquals(1, database.getEntryCount());
        assertEquals("Merged title", database.getEntryByCitationKey("Key").orElseThrow().getField(StandardField.TITLE).orElseThrow());
        assertTrue(undoManager.hasChanged());
        verify(libraryTab).markBaseChanged();
        verify(libraryTab, never()).resetChangedProperties();
    }

    @Test
    void applyResolvedChangesAppliesDiskVersionAndKeepsLibraryClean() {
        BibEntry oldEntry = new BibEntry().withCitationKey("Key")
                                          .withField(StandardField.TITLE, "Old title");
        BibDatabase database = new BibDatabase(List.of(oldEntry));
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        BibEntry diskEntry = new BibEntry(oldEntry).withField(StandardField.TITLE, "Disk title");
        EntryChange diskChange = new EntryChange(oldEntry, diskEntry, databaseContext);
        diskChange.accept();

        UndoManager undoManager = new UndoManager();
        undoManager.markUnchanged();
        LibraryTab libraryTab = mock(LibraryTab.class);
        DatabaseChangeMonitor monitor = new DatabaseChangeMonitor(
                databaseContext,
                mock(FileUpdateMonitor.class),
                mock(TaskExecutor.class),
                mock(DialogService.class),
                mock(GuiPreferences.class),
                undoManager,
                mock(StateManager.class),
                libraryTab);

        monitor.applyResolvedChanges(List.of(diskChange), true);

        assertEquals(1, database.getEntryCount());
        assertEquals("Disk title", database.getEntryByCitationKey("Key").orElseThrow().getField(StandardField.TITLE).orElseThrow());
        assertTrue(undoManager.hasChanged());
        verify(libraryTab).resetChangedProperties();
        verify(libraryTab, never()).markBaseChanged();
    }

    @Test
    @SuppressWarnings("unchecked")
    void notifyExternalChangesReplacesPreviousNotification(@TempDir Path tempDir) throws Exception {
        Path monitoredPath = tempDir.resolve("library.bib");
        Files.writeString(monitoredPath, "@misc{a,}");

        NotificationGroup<Path, Notifications.FileNotification> fileNotifications = new NotificationGroup<>("Files");
        DialogService dialogService = mock(DialogService.class);
        doAnswer(invocation -> {
            Notifications.FileNotification fileNotification = invocation.getArgument(0, Notifications.FileNotification.class);
            fileNotifications.getNotifications().add(fileNotification);
            return null;
        }).when(dialogService).notify(any(Notifications.FileNotification.class));

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(monitoredPath));

        DatabaseChangeMonitor monitor = new DatabaseChangeMonitor(
                databaseContext,
                mock(FileUpdateMonitor.class),
                mock(TaskExecutor.class),
                dialogService,
                mock(GuiPreferences.class),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(LibraryTab.class));

        DatabaseChange firstChange = new EntryAdd(new BibEntry().withCitationKey("first"), new BibDatabaseContext(), null);
        DatabaseChange secondChange = new EntryAdd(new BibEntry().withCitationKey("second"), new BibDatabaseContext(), null);

        monitor.notifyExternalChanges(List.of(firstChange));
        monitor.notifyExternalChanges(List.of(secondChange));

        assertEquals(1, fileNotifications.getNotifications().size());
    }
}
