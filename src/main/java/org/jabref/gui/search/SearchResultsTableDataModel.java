package org.jabref.gui.search;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.StateManager;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableFieldValueFormatter;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.FilteredListProxy;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchQuery;
import org.jabref.model.search.SearchResults;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class SearchResultsTableDataModel {

    private final ObservableList<BibEntryTableViewModel> entriesViewModel = FXCollections.observableArrayList();
    private final SortedList<BibEntryTableViewModel> entriesSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter;
    private final StateManager stateManager;
    private final FilteredList<BibEntryTableViewModel> entriesFiltered;
    private final TaskExecutor taskExecutor;

    public SearchResultsTableDataModel(BibDatabaseContext bibDatabaseContext, PreferencesService preferencesService, StateManager stateManager, TaskExecutor taskExecutor) {
        NameDisplayPreferences nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.fieldValueFormatter = new SimpleObjectProperty<>(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));

        populateEntriesViewModel();
        stateManager.getOpenDatabases().addListener((ListChangeListener<BibDatabaseContext>) change -> populateEntriesViewModel());
        entriesFiltered = new FilteredList<>(entriesViewModel, BibEntryTableViewModel::isVisible);

        // We need to wrap the list since otherwise sorting in the table does not work
        entriesSorted = new SortedList<>(entriesFiltered);

        EasyBind.listen(stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH), (observable, oldValue, newValue) -> updateSearchMatches(newValue));
        stateManager.searchResultSize(SearchType.GLOBAL_SEARCH).bind(Bindings.size(entriesFiltered));
    }

    private void populateEntriesViewModel() {
        entriesViewModel.clear();
        for (BibDatabaseContext context : stateManager.getOpenDatabases()) {
            ObservableList<BibEntry> entriesForDb = context.getDatabase().getEntries();
            ObservableList<BibEntryTableViewModel> viewModelForDb = EasyBind.mapBacked(entriesForDb, entry -> new BibEntryTableViewModel(entry, context, fieldValueFormatter), false);
            entriesViewModel.addAll(viewModelForDb);
        }
    }

    private void updateSearchMatches(Optional<SearchQuery> query) {
        BackgroundTask.wrap(() -> {
            if (query.isPresent()) {
                SearchResults searchResults = new SearchResults();
                for (BibDatabaseContext context : stateManager.getOpenDatabases()) {
                    stateManager.getLuceneManager(context).ifPresent(luceneManager -> {
                        searchResults.mergeSearchResults(luceneManager.search(query.get()));
                    });
                }
                for (BibEntryTableViewModel entry : entriesViewModel) {
                    entry.searchScoreProperty().set(searchResults.getSearchScoreForEntry(entry.getEntry()));
                    entry.hasFullTextResultsProperty().set(searchResults.hasFulltextResults(entry.getEntry()));
                    entry.isVisibleBySearch().set(entry.searchScoreProperty().get() > 0);
                }
            } else {
                for (BibEntryTableViewModel entry : entriesViewModel) {
                    entry.searchScoreProperty().set(0);
                    entry.hasFullTextResultsProperty().set(false);
                    entry.isVisibleBySearch().set(true);
                }
            }
        }).onSuccess(result -> FilteredListProxy.refilterListReflection(entriesFiltered)).executeWith(taskExecutor);
    }

    public SortedList<BibEntryTableViewModel> getEntriesFilteredAndSorted() {
        return entriesSorted;
    }
}
