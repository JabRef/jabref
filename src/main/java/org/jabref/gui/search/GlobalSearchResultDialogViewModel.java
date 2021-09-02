package org.jabref.gui.search;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.StateManager;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.tobiasdiez.easybind.EasyBind;

public class GlobalSearchResultDialogViewModel {

    private final StateManager stateManager;
    private final BibDatabaseContext searchDatabaseContext = new BibDatabaseContext();
    private final BooleanProperty keepOnTop = new SimpleBooleanProperty();
    private SearchPreferences searchPreferences;

    public GlobalSearchResultDialogViewModel(StateManager stateManager, PreferencesService preferencesService) {
        this.stateManager = stateManager;
        searchPreferences = preferencesService.getSearchPreferences();

        keepOnTop.set(searchPreferences.isKeepWindowOnTop());

        EasyBind.subscribe(this.keepOnTop, selected -> {
            searchPreferences = searchPreferences.withKeepGlobalSearchDialogOnTop(selected);
            preferencesService.storeSearchPreferences(searchPreferences);
        });
    }

    public void updateSearch() {
        BibDatabaseContext resultContext = new BibDatabaseContext();
        for (BibDatabaseContext dbContext : this.stateManager.getOpenDatabases()) {
            List<BibEntry> result = dbContext.getEntries().stream()
                                             .filter(entry -> isMatchedBySearch(stateManager.activeSearchQueryProperty().get(), entry))
                                             .collect(Collectors.toList());
            resultContext.getDatabase().insertEntries(result);
        }
        this.addEntriesToBibContext(resultContext);
    }

    private void addEntriesToBibContext(BibDatabaseContext context) {
        List<BibEntry> toBeRemoved = this.searchDatabaseContext.getDatabase().getEntries();
        this.searchDatabaseContext.getDatabase().removeEntries(toBeRemoved);
        this.searchDatabaseContext.getDatabase().insertEntries(context.getEntries());
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntry entry) {
        return query.map(matcher -> matcher.isMatch(entry))
                    .orElse(true);
    }

    public BibDatabaseContext getSearchDatabaseContext() {
        return searchDatabaseContext;
    }

    public BooleanProperty keepOnTop() {
        return this.keepOnTop;
    }
}
