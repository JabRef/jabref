package org.jabref.gui.preferences.fetcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.importer.FetcherApiPreferences;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

public class FetcherTabViewModel implements PreferenceTabViewModel {

    private final String WORLDCAT_REQUEST_URL = "https://platform.worldcat.org/wskey";

    private final BooleanProperty useWorldcatKeyProperty = new SimpleBooleanProperty();
    private final StringProperty worldcatKeyProperty = new SimpleStringProperty();

    private final PreferencesService preferencesService;
    private final FetcherApiPreferences initialFetcherApiPreferences;

    public FetcherTabViewModel(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
        this.initialFetcherApiPreferences = preferencesService.getApiKeyPreferences();
    }

    @Override
    public void setValues() {
        if (StringUtil.isNotBlank(initialFetcherApiPreferences.getWorldcatKey())) {
            useWorldcatKeyProperty.setValue(true);
            worldcatKeyProperty.setValue(initialFetcherApiPreferences.getWorldcatKey());
        }
    }

    @Override
    public void storeSettings() {
        Map<String, String> keys = new HashMap<>();

        keys.put("worldcat", useWorldcatKeyProperty.getValue() ? worldcatKeyProperty.getValue() : "");

        preferencesService.storeApiKeyPreferences(new FetcherApiPreferences(
                keys.get("worldcat"))
        );
    }

    public void openWorldcatWebpage() {
        JabRefDesktop.openBrowserShowPopup(WORLDCAT_REQUEST_URL);
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return Collections.emptyList();
    }

    public BooleanProperty getUseWorldcatKeyProperty() {
        return useWorldcatKeyProperty;
    }

    public StringProperty getWorldcatKeyProperty() {
        return worldcatKeyProperty;
    }
}
