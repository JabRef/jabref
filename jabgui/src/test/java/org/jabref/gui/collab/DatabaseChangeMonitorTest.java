package org.jabref.gui.collab;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                mock(StateManager.class));

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
                mock(StateManager.class));
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
}
