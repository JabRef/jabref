package org.jabref.gui.importer.actions;

import com.google.common.jimfs.Jimfs;
import org.jabref.gui.*;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class OpenDatabaseActionTest {
    DialogService dialogService;
    GuiPreferences guiPreferences;
    OpenDatabaseAction openDatabaseAction;


    @BeforeEach
    void initializeOpenDatabaseAction() {
        dialogService = mock(DialogService.class);
        guiPreferences = mock(GuiPreferences.class);
        openDatabaseAction = spy(new OpenDatabaseAction(
                mock(LibraryTabContainer.class),
                guiPreferences,
                mock(AiService.class),
                dialogService,
                mock(StateManager.class),
                mock(FileUpdateMonitor.class),
                mock(BibEntryTypesManager.class),
                mock(CountingUndoManager.class),
                mock(ClipBoardManager.class),
                mock(TaskExecutor.class)
        ));
    }

    @Test
    void testGetFilesToOpenFailsToOpenPath() {
        FileSystem fs = Jimfs.newFileSystem();
        Path path = fs.getPath("test-dir");

        FilePreferences filePreferences = mock(FilePreferences.class);
        FileDialogConfiguration badConfig = mock(FileDialogConfiguration.class);
        FileDialogConfiguration goodConfig = mock(FileDialogConfiguration.class);

        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(openDatabaseAction.getInitialDirectory()).thenReturn(path);

        // Make it so that showFileOpenDialogAndGetMultipleFiles will throw an error when called with the bad path, but
        // not for the good path as in issue #10548
        when(dialogService.showFileOpenDialogAndGetMultipleFiles(badConfig))
                .thenAnswer(x -> { throw new IllegalArgumentException(); });
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
}
