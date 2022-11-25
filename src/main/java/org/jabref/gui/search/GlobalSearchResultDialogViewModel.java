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

    public GlobalSearchResultDialogViewModel(PreferencesService preferencesService) {
        SearchPreferences searchPreferences = preferencesService.getSearchPreferences();

        keepOnTop.set(searchPreferences.shouldKeepWindowOnTop());

        EasyBind.subscribe(this.keepOnTop, searchPreferences::setKeepWindowOnTop);
    }

    public BibDatabaseContext getSearchDatabaseContext() {
        return searchDatabaseContext;
    }

    public BooleanProperty keepOnTop() {
        return this.keepOnTop;
    }
}
