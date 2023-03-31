package org.jabref.gui.search;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.tobiasdiez.easybind.EasyBind;

public class GlobalSearchResultDialogViewModel {
    SearchPreferences searchPreferences;
    private final BibDatabaseContext searchDatabaseContext = new BibDatabaseContext();
    private final BooleanProperty keepOnTop = new SimpleBooleanProperty();
    private final IntegerProperty searchWindowHeight = new SimpleIntegerProperty();
    private final IntegerProperty searchWindowWidth = new SimpleIntegerProperty();

    public GlobalSearchResultDialogViewModel(PreferencesService preferencesService) {
        searchPreferences = preferencesService.getSearchPreferences();

        keepOnTop.set(searchPreferences.shouldKeepWindowOnTop());
        searchWindowHeight.set(searchPreferences.getSearchWindowHeight());
        searchWindowWidth.set(searchPreferences.getSearchWindowWidth());

        EasyBind.subscribe(this.keepOnTop, searchPreferences::setKeepWindowOnTop);
    }

    public BibDatabaseContext getSearchDatabaseContext() {
        return searchDatabaseContext;
    }

    public BooleanProperty keepOnTop() {
        return this.keepOnTop;
    }

    public int searchWindowHeight() {
        return this.searchWindowHeight.get();
    }

    public int searchWindowWidth() {
        return this.searchWindowWidth.get();
    }

    public void updateWindowSize(int height, int width) {
        searchPreferences.setSearchWindowHeight(height);
        searchPreferences.setSearchWindowWidth(width);
    }
}
