package org.jabref.gui.search;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.tobiasdiez.easybind.EasyBind;

public class GlobalSearchResultDialogViewModel {

    private final BibDatabaseContext searchDatabaseContext = new BibDatabaseContext();
    private final BooleanProperty keepOnTop = new SimpleBooleanProperty();
    private SearchPreferences searchPreferences;

    public GlobalSearchResultDialogViewModel(PreferencesService preferencesService) {
        searchPreferences = preferencesService.getSearchPreferences();

        keepOnTop.set(searchPreferences.isKeepWindowOnTop());

        EasyBind.subscribe(this.keepOnTop, selected -> {
            searchPreferences = searchPreferences.withKeepGlobalSearchDialogOnTop(selected);
            preferencesService.storeSearchPreferences(searchPreferences);
        });
    }

    public BibDatabaseContext getSearchDatabaseContext() {
        return searchDatabaseContext;
    }

    public BooleanProperty keepOnTop() {
        return this.keepOnTop;
    }
}
