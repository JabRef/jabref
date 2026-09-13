package org.jabref.gui.search;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.search.NoOpSearchBackend;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.search.inmemory.InMemorySearchBackend;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(JavaFxExtension.class)
class SearchResultsTableDataModelTest {

    private StateManager stateManager;
    private GuiPreferences preferences;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        stateManager = new JabRefGuiStateManager();
        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        taskExecutor = new CurrentThreadTaskExecutor();
    }

    private void registerSearchContext(BibDatabaseContext context) {
        SearchContext searchContext = new SearchContext(
                new SimpleBooleanProperty(false),
                NoOpSearchBackend::new,
                () -> new InMemorySearchBackend(context, new BibEntryPreferences(','))
        );
        stateManager.setSearchContext(context, searchContext);
    }

    @Test
    void openingNewDatabaseUpdatesSearchMatchesWithActiveGlobalSearchQuery() {
        BibDatabaseContext context1 = new BibDatabaseContext();
        registerSearchContext(context1);
        BibEntry entry1 = new BibEntry().withField(StandardField.AUTHOR, "Alice");
        context1.getDatabase().insertEntry(entry1);
        stateManager.getOpenDatabases().add(context1);

        stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH).set(Optional.of(new SearchQuery("Alice")));

        SearchResultsTableDataModel model = new SearchResultsTableDataModel(
                context1,
                preferences,
                stateManager,
                taskExecutor);

        List<BibEntry> initialMatched = model.getEntriesFilteredAndSorted().stream()
                                             .map(BibEntryTableViewModel::getEntry)
                                             .toList();
        assertEquals(List.of(entry1), initialMatched);

        BibDatabaseContext context2 = new BibDatabaseContext();
        registerSearchContext(context2);
        BibEntry entry2 = new BibEntry().withField(StandardField.AUTHOR, "Bob");
        BibEntry entry3 = new BibEntry().withField(StandardField.AUTHOR, "Alice");
        context2.getDatabase().insertEntries(entry2, entry3);

        stateManager.getOpenDatabases().add(context2);

        List<BibEntry> updatedMatched = model.getEntriesFilteredAndSorted().stream()
                                             .map(BibEntryTableViewModel::getEntry)
                                             .toList();
        assertEquals(List.of(entry1, entry3), updatedMatched);
    }
}
