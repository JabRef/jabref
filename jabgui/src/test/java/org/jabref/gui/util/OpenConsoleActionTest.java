package org.jabref.gui.util;

import java.util.Optional;

import org.jabref.gui.StateManager;
import org.jabref.gui.frame.OpenConsoleAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenConsoleActionTest {

    private final StateManager stateManager = mock(StateManager.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final BibDatabaseContext current = mock(BibDatabaseContext.class);
    private final BibDatabaseContext other = mock(BibDatabaseContext.class);

    @BeforeEach
    void setup() {
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(current));
    }

    @Test
    void newActionGetsCurrentDatabase() {
        OpenConsoleAction action = new OpenConsoleAction(stateManager, preferences, null);
        action.execute();
        verify(stateManager, times(1)).getActiveDatabase();
        verify(current, times(1)).getDatabasePath();
    }

    @Test
    void newActionGetsSuppliedDatabase() {
        OpenConsoleAction action = new OpenConsoleAction(() -> other, stateManager, preferences, null);
        action.execute();
        verify(stateManager, never()).getActiveDatabase();
        verify(other, times(1)).getDatabasePath();
    }

    @Test
    void actionDefaultsToCurrentDatabase() {
        OpenConsoleAction action = new OpenConsoleAction(() -> null, stateManager, preferences, null);
        action.execute();
        verify(stateManager, times(1)).getActiveDatabase();
        verify(current, times(1)).getDatabasePath();
    }
}
