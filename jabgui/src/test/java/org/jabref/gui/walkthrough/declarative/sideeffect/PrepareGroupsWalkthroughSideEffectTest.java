package org.jabref.gui.walkthrough.declarative.sideeffect;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.jabref.gui.StateManager;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.walkthrough.Walkthrough;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class PrepareGroupsWalkthroughSideEffectTest extends JavaFxTest {
    private TextField searchField;

    @Override
    public void start(Stage stage) {
        searchField = new TextField();
    }

    @Test
    void makesGroupsPaneVisibleAndRestoresSearchField() {
        ObservableList<SidePaneType> visibleSidePanes = FXCollections.observableArrayList();
        PrepareGroupsWalkthroughSideEffect sideEffect = new PrepareGroupsWalkthroughSideEffect(
                stateManager(visibleSidePanes), () -> Optional.of(searchField));

        JavaFxExtension.invokeAndWait(() -> {
            searchField.setText("no matching entries");
            sideEffect.forward(mock(Walkthrough.class));
        });

        assertEquals(List.of(SidePaneType.GROUPS), visibleSidePanes);
        assertEquals("", searchField.getText());

        JavaFxExtension.invokeAndWait(() -> sideEffect.backward(mock(Walkthrough.class)));

        assertEquals(List.of(), visibleSidePanes);
        assertEquals("no matching entries", searchField.getText());
    }

    @Test
    void keepsGroupsPaneVisibleWhenItWasAlreadyVisible() {
        ObservableList<SidePaneType> visibleSidePanes = FXCollections.observableArrayList(SidePaneType.GROUPS);
        PrepareGroupsWalkthroughSideEffect sideEffect = new PrepareGroupsWalkthroughSideEffect(
                stateManager(visibleSidePanes), () -> Optional.of(searchField));

        JavaFxExtension.invokeAndWait(() -> {
            sideEffect.forward(mock(Walkthrough.class));
            sideEffect.backward(mock(Walkthrough.class));
        });

        assertEquals(List.of(SidePaneType.GROUPS), visibleSidePanes);
    }

    private StateManager stateManager(ObservableList<SidePaneType> visibleSidePanes) {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getVisibleSidePaneComponents()).thenReturn(visibleSidePanes);
        return stateManager;
    }
}
