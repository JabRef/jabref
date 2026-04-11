package org.jabref.gui.search;

import java.util.EnumSet;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class GlobalSearchBarTest {
    private HBox hBox;
    private TextField focusTargetField;

    private StateManager stateManager;

    @Start
    public void onStart(Stage stage) {
        SearchPreferences searchPreferences = mock(SearchPreferences.class);
        when(searchPreferences.getSearchFlags()).thenReturn(EnumSet.noneOf(SearchFlags.class));
        when(searchPreferences.getObservableSearchFlags()).thenReturn(FXCollections.observableSet());
        when(searchPreferences.keepSearchStringProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(searchPreferences.searchDisplayModeProperty()).thenReturn(new SimpleObjectProperty<>(SearchDisplayMode.FLOAT));
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getSearchPreferences()).thenReturn(searchPreferences);

        KeyBindingRepository keyBindingRepository = mock(KeyBindingRepository.class);
        when(keyBindingRepository.matches(any(), any())).thenReturn(false);
        when(preferences.getKeyBindingRepository()).thenReturn(keyBindingRepository);

        stateManager = new JabRefGuiStateManager();
        // Need for active database, otherwise the searchField will be disabled
        stateManager.setActiveDatabase(new BibDatabaseContext());

        // Instantiate GlobalSearchBar class, so the change listener is registered
        GlobalSearchBar searchBar = new GlobalSearchBar(
                mock(LibraryTabContainer.class),
                stateManager,
                preferences,
                mock(CountingUndoManager.class),
                mock(DialogService.class),
                SearchType.NORMAL_SEARCH
        );

        focusTargetField = new TextField();
        hBox = new HBox(searchBar, focusTargetField);

        Scene scene = new Scene(hBox, 400, 400);
        stage.setScene(scene);

        stage.show();
    }

    @Test
    void recordingSearchQueriesOnFocusLostOnly(FxRobot robot) throws InterruptedException {
        stateManager.clearSearchHistory();
        String searchQuery = "Smith";
        // Track the node where the search query is entered.
        TextInputControl searchField = robot.lookup("#searchField").queryTextInputControl();
        UiTaskExecutor.runAndWaitInJavaFXThread(searchField::requestFocus);

        for (char c : searchQuery.toCharArray()) {
            UiTaskExecutor.runAndWaitInJavaFXThread(() -> searchField.appendText(String.valueOf(c)));
            Thread.sleep(401);
            assertTrue(stateManager.getWholeSearchHistory().isEmpty());
        }

        // Set the focus to another node to trigger the listener and finally record the query.
        UiTaskExecutor.runAndWaitInJavaFXThread(focusTargetField::requestFocus);
        for (int i = 0; i < 10 && stateManager.getWholeSearchHistory().isEmpty(); i++) {
            Thread.sleep(100);
        }
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory().stream().toList();

        assertEquals(List.of("Smith"), lastSearchHistory);
    }

    @Test
    void emptyQueryIsNotRecorded(FxRobot robot) {
        stateManager.clearSearchHistory();
        String searchQuery = "";
        TextInputControl searchField = robot.lookup("#searchField").queryTextInputControl();

        UiTaskExecutor.runAndWaitInJavaFXThread(() -> searchField.setText(searchQuery));

        UiTaskExecutor.runAndWaitInJavaFXThread(focusTargetField::requestFocus);
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory().stream().toList();

        assertEquals(List.of(), lastSearchHistory);
    }
}
