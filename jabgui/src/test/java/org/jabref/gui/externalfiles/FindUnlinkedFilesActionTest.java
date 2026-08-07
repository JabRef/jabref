package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class FindUnlinkedFilesActionTest {

    @Mock
    private DialogService dialogService;

    @Mock
    private BibDatabaseContext databaseContext;

    private StateManager stateManager;
    private FindUnlinkedFilesAction action;

    @Mock private GuiPreferences preferences;
    @Mock private TaskExecutor taskExecutor;
    @Mock private UndoManager undoManager;
    @Mock private FileUpdateMonitor fileUpdateMonitor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stateManager = new JabRefGuiStateManager();
        action = new FindUnlinkedFilesAction(dialogService, stateManager);
    }

    @Test
    void isEnabledWhenNewDatabaseIsSavedTest() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(Path.of("test.bib")));
        when(databaseContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);

        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        assertTrue(action.executableProperty().get());
    }

    @Test
    void isDisabledWhenNewDatabasePathIsEmptyTest() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.empty());
        when(databaseContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);

        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        assertFalse(action.executableProperty().get());
    }

    @Test
    void isDisabledWhenNewDatabaseIsNotLocalTest() {
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(Path.of("test.bib")));
        when(databaseContext.getLocation()).thenReturn(DatabaseLocation.SHARED);

        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        assertFalse(action.executableProperty().get());
    }
}
