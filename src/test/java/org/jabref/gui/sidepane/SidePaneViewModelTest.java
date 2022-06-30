package org.jabref.gui.sidepane;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SidePanePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class SidePaneViewModelTest {

    PreferencesService preferencesService = mock(PreferencesService.class);
    StateManager stateManager = mock(StateManager.class);
    TaskExecutor taskExecutor = mock(TaskExecutor.class);
    DialogService dialogService = mock(DialogService.class);
    UndoManager undoManager = mock(UndoManager.class);

    SidePanePreferences sidePanePreferences = new SidePanePreferences(new HashSet<>(), new HashMap<>(), 0);
    ObservableList<SidePaneType> sidePaneComponents = FXCollections.observableArrayList();
    SidePaneViewModel sidePaneViewModel;

    @BeforeEach
    void setUp() {
        when(stateManager.getVisibleSidePaneComponents()).thenReturn(sidePaneComponents);
        when(stateManager.getLocalDragboard()).thenReturn(mock(CustomLocalDragboard.class));
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        when(preferencesService.getSidePanePreferences()).thenReturn(sidePanePreferences);

        sidePanePreferences.visiblePanes().addAll(EnumSet.allOf(SidePaneType.class));
        sidePanePreferences.getPreferredPositions().put(SidePaneType.GROUPS, 0);
        sidePanePreferences.getPreferredPositions().put(SidePaneType.WEB_SEARCH, 1);
        sidePanePreferences.getPreferredPositions().put(SidePaneType.OPEN_OFFICE, 2);

        sidePaneViewModel = new SidePaneViewModel(preferencesService, stateManager, taskExecutor, dialogService, undoManager);
    }

    @Test
    void moveUp() {
        sidePaneViewModel.moveUp(SidePaneType.WEB_SEARCH);

        assertEquals(sidePaneComponents.get(0), SidePaneType.WEB_SEARCH);
        assertEquals(sidePaneComponents.get(1), SidePaneType.GROUPS);
    }

    @Test
    void moveUpFromFirstPosition() {
        sidePaneViewModel.moveUp(SidePaneType.GROUPS);

        assertEquals(sidePaneComponents.get(0), SidePaneType.GROUPS);
    }

    @Test
    void moveDown() {
        sidePaneViewModel.moveDown(SidePaneType.WEB_SEARCH);

        assertEquals(sidePaneComponents.get(1), SidePaneType.OPEN_OFFICE);
        assertEquals(sidePaneComponents.get(2), SidePaneType.WEB_SEARCH);
    }

    @Test
    void moveDownFromLastPosition() {
        sidePaneViewModel.moveDown(SidePaneType.OPEN_OFFICE);

        assertEquals(sidePaneComponents.get(2), SidePaneType.OPEN_OFFICE);
    }

    @Test
    void sortByPreferredPositions() {
        sidePanePreferences.getPreferredPositions().put(SidePaneType.GROUPS, 2);
        sidePanePreferences.getPreferredPositions().put(SidePaneType.OPEN_OFFICE, 0);

        sidePaneComponents.sort(new SidePaneViewModel.PreferredIndexSort(sidePanePreferences));

        assertTrue(sidePaneComponents.get(0) == SidePaneType.OPEN_OFFICE && sidePaneComponents.get(2) == SidePaneType.GROUPS);
    }
}
