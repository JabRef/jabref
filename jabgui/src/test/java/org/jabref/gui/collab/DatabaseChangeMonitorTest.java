package org.jabref.gui.collab;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
}
