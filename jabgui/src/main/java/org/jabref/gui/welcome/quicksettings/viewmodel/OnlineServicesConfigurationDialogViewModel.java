package org.jabref.gui.welcome.quicksettings.viewmodel;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.slr.StudyCatalogItem;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.CompositeSearchBasedFetcher;

public class OnlineServicesConfigurationDialogViewModel extends AbstractViewModel {
    private final BooleanProperty versionCheckProperty = new SimpleBooleanProperty();
    private final BooleanProperty webSearchProperty = new SimpleBooleanProperty();
    private final BooleanProperty grobidEnabledProperty = new SimpleBooleanProperty();
    private final StringProperty grobidUrlProperty = new SimpleStringProperty("");

    private final ListProperty<StudyCatalogItem> fetchersProperty =
            new SimpleListProperty<>(FXCollections.observableArrayList());

    private final GuiPreferences preferences;

    public OnlineServicesConfigurationDialogViewModel(GuiPreferences preferences) {
        this.preferences = preferences;

        initializeSettings();
        initializeFetchers();
    }

    private void initializeSettings() {
        versionCheckProperty.set(preferences.getInternalPreferences().isVersionCheckEnabled());
        webSearchProperty.set(preferences.getImporterPreferences().areImporterEnabled());
        grobidEnabledProperty.set(preferences.getGrobidPreferences().isGrobidEnabled());
        grobidUrlProperty.set(preferences.getGrobidPreferences().getGrobidURL());
    }

    private void initializeFetchers() {
        List<StudyCatalogItem> availableFetchers = WebFetchers
                .getSearchBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences())
                .stream()
                .map(SearchBasedFetcher::getName)
                .filter(name -> !CompositeSearchBasedFetcher.FETCHER_NAME.equals(name))
                .map(name -> {
                    boolean enabled = preferences.getImporterPreferences().getCatalogs().contains(name);
                    return new StudyCatalogItem(name, enabled);
                })
                .toList();

        fetchersProperty.setAll(availableFetchers);
    }

    public BooleanProperty versionCheckProperty() {
        return versionCheckProperty;
    }

    public boolean isVersionCheckEnabled() {
        return versionCheckProperty.get();
    }

    public BooleanProperty webSearchProperty() {
        return webSearchProperty;
    }

    public boolean isWebSearchEnabled() {
        return webSearchProperty.get();
    }

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabledProperty;
    }

    public boolean isGrobidEnabled() {
        return grobidEnabledProperty.get();
    }

    public StringProperty grobidUrlProperty() {
        return grobidUrlProperty;
    }

    public String getGrobidUrl() {
        return grobidUrlProperty.get();
    }

    public ListProperty<StudyCatalogItem> fetchersProperty() {
        return fetchersProperty;
    }

    public void saveSettings() {
        preferences.getInternalPreferences().setVersionCheckEnabled(isVersionCheckEnabled());
        preferences.getImporterPreferences().setImporterEnabled(isWebSearchEnabled());
        preferences.getGrobidPreferences().setGrobidEnabled(isGrobidEnabled());
        preferences.getGrobidPreferences().setGrobidURL(getGrobidUrl());

        List<String> enabledFetchers = new ArrayList<>();
        for (StudyCatalogItem fetcher : fetchersProperty) {
            if (fetcher.isEnabled()) {
                enabledFetchers.add(fetcher.getName());
            }
        }
        preferences.getImporterPreferences().setCatalogs(enabledFetchers);
    }
}
