package org.jabref.gui.sidepane;

import java.util.HashMap;
import java.util.HashSet;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.gui.frame.SidePanePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToggleSidePaneActionTest {

    StateManager stateManager = mock(StateManager.class);
    ObservableList<SidePaneType> visibleComponents = FXCollections.observableArrayList();
    SidePanePreferences sidePanePreferences = new SidePanePreferences(new HashSet<>(), new HashMap<>(), 0);
    ToggleSidePaneAction action;

    @BeforeEach
    void setUp() {
        when(stateManager.getVisibleSidePaneComponents()).thenReturn(visibleComponents);
        action = new ToggleSidePaneAction(stateManager, sidePanePreferences);
    }

    @Test
    void hidesAllVisibleComponents() {
        visibleComponents.addAll(SidePaneType.GROUPS, SidePaneType.WEB_SEARCH);

        action.execute();

        assertTrue(visibleComponents.isEmpty());
    }

    @Test
    void restoresExactlyWhatWasHidden() {
        visibleComponents.addAll(SidePaneType.GROUPS, SidePaneType.WEB_SEARCH);

        action.execute(); // hide
        action.execute(); // restore

        assertEquals(2, visibleComponents.size());
        assertTrue(visibleComponents.contains(SidePaneType.GROUPS));
        assertTrue(visibleComponents.contains(SidePaneType.WEB_SEARCH));
    }

    @Test
    void fallsBackToPreferencesDefaultWhenNothingWasHiddenThisSession() {
        sidePanePreferences.setVisiblePanes(java.util.EnumSet.of(SidePaneType.OPEN_OFFICE));

        action.execute(); // nothing visible, nothing remembered → fall back

        assertEquals(1, visibleComponents.size());
        assertTrue(visibleComponents.contains(SidePaneType.OPEN_OFFICE));
    }
}
