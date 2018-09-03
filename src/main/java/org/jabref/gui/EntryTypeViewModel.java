package org.jabref.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.preferences.JabRefPreferences;

public class EntryTypeViewModel {

    private final JabRefPreferences preferences;
    private final BooleanProperty searchingProperty = new SimpleBooleanProperty();
    private final ObjectProperty<IdBasedFetcher> selectedItemProperty = new SimpleObjectProperty<>();
    private final ListProperty<IdBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());

    public EntryTypeViewModel(JabRefPreferences preferences) {
        this.preferences = preferences;
        fetchers.addAll(WebFetchers.getIdBasedFetchers(preferences.getImportFormatPreferences()));
        selectedItemProperty.setValue(getLastSelectedFetcher());
    }

    public BooleanProperty searchingProperty() {
        return searchingProperty;
    }

    public ObjectProperty<IdBasedFetcher> selectedItemProperty() {
        return selectedItemProperty;
    }

    public void storeSelectedFetcher() {
        preferences.setIdBasedFetcherForEntryGenerator(selectedItemProperty.getValue().getName());
    }

    private IdBasedFetcher getLastSelectedFetcher() {
        return fetchers.stream().filter(fetcher -> fetcher.getName().equals(preferences.getIdBasedFetcherForEntryGenerator()))
                       .findFirst().orElse(new DoiFetcher(preferences.getImportFormatPreferences()));
    }

    public ListProperty<IdBasedFetcher> fetcherItemsProperty() {
        return fetchers;
    }
}
