package org.jabref.gui.entryeditor.fileannotationtab;

import java.util.EnumSet;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.TextFlow;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
class FulltextSearchResultsTabTest {
    @Test
    void completedSearchUpdatesTheOpenTab() {
        SearchQuery query = new SearchQuery("term", EnumSet.of(SearchFlags.FULLTEXT));
        OptionalObjectProperty<SearchQuery> activeQuery = OptionalObjectProperty.empty();
        activeQuery.setValue(Optional.of(query));

        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(activeQuery);
        OptionalObjectProperty<LibraryTab> activeTab = OptionalObjectProperty.empty();
        LibraryTab libraryTab = mock(LibraryTab.class);
        ReadOnlyObjectWrapper<Optional<SearchResults>> results = new ReadOnlyObjectWrapper<>(Optional.empty());
        when(libraryTab.searchResultsProperty()).thenReturn(results.getReadOnlyProperty());
        activeTab.setValue(Optional.of(libraryTab));
        when(stateManager.activeTabProperty()).thenReturn(activeTab);

        FulltextSearchResultsTab tab = new FulltextSearchResultsTab(
                stateManager, mock(GuiPreferences.class), mock(DialogService.class), mock(TaskExecutor.class));
        tab.bindToEntry(new BibEntry());
        TextFlow content = (TextFlow) ((ScrollPane) tab.getContent()).getContent();

        assertEquals(0, content.getChildren().size());

        results.set(Optional.of(new SearchResults()));

        assertEquals(1, content.getChildren().size());
        tab.dispose();
    }

    @Test
    void switchingLibrariesStopsObservingPreviousResults() {
        SearchQuery query = new SearchQuery("term", EnumSet.of(SearchFlags.FULLTEXT));
        OptionalObjectProperty<SearchQuery> activeQuery = OptionalObjectProperty.empty();
        activeQuery.setValue(Optional.of(query));

        LibraryTab firstLibrary = mock(LibraryTab.class);
        ReadOnlyObjectWrapper<Optional<SearchResults>> firstResults = new ReadOnlyObjectWrapper<>(Optional.empty());
        when(firstLibrary.searchResultsProperty()).thenReturn(firstResults.getReadOnlyProperty());
        LibraryTab secondLibrary = mock(LibraryTab.class);
        ReadOnlyObjectWrapper<Optional<SearchResults>> secondResults = new ReadOnlyObjectWrapper<>(Optional.empty());
        when(secondLibrary.searchResultsProperty()).thenReturn(secondResults.getReadOnlyProperty());

        OptionalObjectProperty<LibraryTab> activeTab = OptionalObjectProperty.empty();
        activeTab.setValue(Optional.of(firstLibrary));
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(activeQuery);
        when(stateManager.activeTabProperty()).thenReturn(activeTab);

        FulltextSearchResultsTab tab = new FulltextSearchResultsTab(
                stateManager, mock(GuiPreferences.class), mock(DialogService.class), mock(TaskExecutor.class));
        tab.bindToEntry(new BibEntry());
        TextFlow content = (TextFlow) ((ScrollPane) tab.getContent()).getContent();

        activeTab.setValue(Optional.of(secondLibrary));
        secondResults.set(Optional.of(new SearchResults()));
        assertEquals(1, content.getChildren().size());
        Object shownResult = content.getChildren().getFirst();

        firstResults.set(Optional.of(new SearchResults()));
        assertSame(shownResult, content.getChildren().getFirst());
        tab.dispose();
    }
}
