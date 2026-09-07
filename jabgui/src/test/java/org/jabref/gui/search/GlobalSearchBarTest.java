package org.jabref.gui.search;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;

import static org.jabref.gui.testutils.JavaFxExtension.invokeAndWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
class GlobalSearchBarTest {
    private HBox hBox;

    private StateManager stateManager;

    @BeforeEach
    void setUp() {
        invokeAndWait(() -> {
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

                    mock(DialogService.class),
                    SearchType.NORMAL_SEARCH
            );

            hBox = new HBox(searchBar);

            Stage stage = new Stage();
            stage.setScene(new Scene(hBox, 400, 400));
            stage.show();
        });
    }

    @Test
    void recordingSearchQueriesOnFocusLostOnly() throws InterruptedException {
        stateManager.clearSearchHistory();
        String searchQuery = "Smith";
        // Track the node, that the search query will be typed into
        TextInputControl searchField = JavaFxExtension.lookup(hBox, "#searchField", TextInputControl.class);

        // The focus is on searchField node, as we click on the search box
        invokeAndWait(searchField::requestFocus);
        for (char c : searchQuery.toCharArray()) {
            invokeAndWait(() -> searchField.appendText(String.valueOf(c)));
            Thread.sleep(401);
            assertTrue(stateManager.getWholeSearchHistory().isEmpty());
        }

        // Set the focus to another node to trigger the listener and finally record the query.
        invokeAndWait(hBox::requestFocus);
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory().stream().toList();

        assertEquals(List.of("Smith"), lastSearchHistory);
    }

    @Test
    void emptyQueryIsNotRecorded() {
        stateManager.clearSearchHistory();
        String searchQuery = "";
        TextInputControl searchField = JavaFxExtension.lookup(hBox, "#searchField", TextInputControl.class);

        invokeAndWait(searchField::requestFocus);
        invokeAndWait(() -> searchField.appendText(searchQuery));

        invokeAndWait(hBox::requestFocus);
        List<String> lastSearchHistory = stateManager.getWholeSearchHistory().stream().toList();

        assertEquals(List.of(), lastSearchHistory);
    }

    @Test
    void blankQueryClearsActiveSearch() throws InterruptedException {
        TextInputControl searchField = JavaFxExtension.lookup(hBox, "#searchField", TextInputControl.class);

        invokeAndWait(searchField::requestFocus);
        invokeAndWait(() -> searchField.appendText("abc"));
        awaitActiveSearchQuery(Optional.of(new SearchQuery("abc")));
        assertEquals(Optional.of(new SearchQuery("abc")), stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get());

        invokeAndWait(searchField::clear);
        invokeAndWait(() -> searchField.appendText("   "));
        awaitActiveSearchQuery(Optional.empty());

        assertEquals(Optional.empty(), stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get());
    }

    private void awaitActiveSearchQuery(Optional<SearchQuery> expected) throws InterruptedException {
        if (expected.equals(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get())) {
            return;
        }

        CountDownLatch updated = new CountDownLatch(1);
        stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).addListener((_, _, current) -> {
            if (expected.equals(current)) {
                updated.countDown();
            }
        });

        assertTrue(updated.await(5, TimeUnit.SECONDS), "Active search query was not updated in time");
    }
}
