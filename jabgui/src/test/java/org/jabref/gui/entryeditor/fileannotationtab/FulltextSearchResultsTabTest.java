package org.jabref.gui.entryeditor.fileannotationtab;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResult;
import org.jabref.model.search.query.SearchResults;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
@NullMarked
class FulltextSearchResultsTabTest {
    // [utest->req~jabgui.search.fulltext.entry-editor-results~1]
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

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getFilePreferences()).thenReturn(FilePreferences.getDefault());
        FulltextSearchResultsTab tab = new FulltextSearchResultsTab(
                stateManager, preferences, mock(DialogService.class), mock(TaskExecutor.class));
        BibEntry entry = new BibEntry().withFiles(List.of(new LinkedFile("", "paper.pdf", "PDF")));
        tab.bindToEntry(entry);
        TextFlow content = (TextFlow) ((ScrollPane) tab.getContent()).getContent();

        assertEquals(0, content.getChildren().size());

        SearchResults linkedFileResults = new SearchResults();
        linkedFileResults.addSearchResult(entry.getId(), new SearchResult(
                "paper.pdf", "before term after", "", 3, new TermQuery(new Term("content", "term"))));
        results.set(Optional.of(linkedFileResults));

        List<String> renderedText = content.getChildren().stream()
                                           .filter(Text.class::isInstance)
                                           .map(node -> ((Text) node).getText())
                                           .toList();
        assertEquals(Localization.lang("Found match in %0", "paper.pdf") + System.lineSeparator() + System.lineSeparator(), renderedText.getFirst());
        assertEquals("term", renderedText.stream().filter(text -> text.contains("term")).findFirst().orElseThrow());
        assertEquals(Localization.lang("On page %0", 3) + System.lineSeparator() + System.lineSeparator(), renderedText.getLast());
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
