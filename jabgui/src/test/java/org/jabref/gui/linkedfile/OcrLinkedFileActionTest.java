package org.jabref.gui.linkedfile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class OcrLinkedFileActionTest {

    private final BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final TaskExecutor taskExecutor = mock(TaskExecutor.class);
    private final FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);
    private final UndoManager undoManager = mock(UndoManager.class);
    private final StateManager stateManager = mock(StateManager.class);

    @BeforeEach
    void setUp() {
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
    }

    private OcrLinkedFileAction createAction(LinkedFile linkedFile) {
        return new OcrLinkedFileAction(
                linkedFile,
                List.of(new BibEntry()),
                databaseContext,
                dialogService,
                preferences,
                taskExecutor,
                fileUpdateMonitor,
                undoManager,
                stateManager
        );
    }

    @Test
    void isDisabledWhenFileIsBroken() {
        LinkedFile brokenFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(brokenFile.isOnlineLink()).thenReturn(false);
        when(brokenFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.empty());
        when(brokenFile.linkProperty()).thenReturn(new SimpleStringProperty("missing.pdf"));

        OcrLinkedFileAction action = createAction(brokenFile);

        assertFalse(action.isExecutable(), "Perform OCR should be disabled when linked file is broken");
    }

    @Test
    void isEnabledWhenFileExists() {
        LinkedFile existingFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(existingFile.isOnlineLink()).thenReturn(false);
        when(existingFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.of(Path.of("existing.pdf")));
        when(existingFile.linkProperty()).thenReturn(new SimpleStringProperty("existing.pdf"));

        OcrLinkedFileAction action = createAction(existingFile);

        assertTrue(action.isExecutable(), "Perform OCR should be enabled when linked file exists");
    }

    @Test
    void isDisabledWhenFileIsOnlineLink() {
        LinkedFile onlineFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(onlineFile.isOnlineLink()).thenReturn(true);
        when(onlineFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.empty());
        when(onlineFile.linkProperty()).thenReturn(new SimpleStringProperty("https://example.org/test.pdf"));

        OcrLinkedFileAction action = createAction(onlineFile);

        assertFalse(action.isExecutable(), "Perform OCR should be disabled for online links");
    }
}
