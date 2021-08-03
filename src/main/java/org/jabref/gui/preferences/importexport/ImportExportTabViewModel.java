package org.jabref.gui.preferences.importexport;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.importer.importsettings.ImportSettingsPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.preferences.CustomApiKeyPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.preferences.PreferencesService;

public class ImportExportTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty generateKeyOnImportProperty = new SimpleBooleanProperty();

    private final static Map<String, String> API_KEY_NAME_URL = new TreeMap<>();
    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    private final ListProperty<CustomApiKeyPreferences> customApiKeyPreferencesListProperty = new SimpleListProperty<>();
    private final ObjectProperty<CustomApiKeyPreferences> selectedCustomApiKeyPreferencesProperty = new SimpleObjectProperty<>();
    private final BooleanProperty useCustomApiKeyProperty = new SimpleBooleanProperty();
    private final StringProperty customApiKeyTextProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    private final DOIPreferences initialDOIPreferences;
    private final ImportSettingsPreferences initialImportSettingsPreferences;

    public ImportExportTabViewModel(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
        this.initialImportSettingsPreferences = preferencesService.getImportSettingsPreferences();
        this.initialDOIPreferences = preferencesService.getDOIPreferences();
    }

    public static void registerApiKeyCustom(String key, String testUrlWithoutApiKey) {
        API_KEY_NAME_URL.put(key, testUrlWithoutApiKey);
    }

    public ArrayList<String> getCustomApiKeyFetchers() {
        return new ArrayList<>(API_KEY_NAME_URL.keySet());
    }

    @Override
    public void setValues() {
        generateKeyOnImportProperty.setValue(initialImportSettingsPreferences.generateNewKeyOnImport());
        useCustomDOIProperty.setValue(initialDOIPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(initialDOIPreferences.getDefaultBaseURI());

        // API keys
        ArrayList<CustomApiKeyPreferences> customApiKeyPreferencesList = new ArrayList<>();
        for (String name : API_KEY_NAME_URL.keySet()) {
            customApiKeyPreferencesList.add(preferencesService.getCustomApiKeyPreferences(name));
        }
        customApiKeyPreferencesListProperty.setValue(FXCollections.observableArrayList(customApiKeyPreferencesList));
        selectedCustomApiKeyPreferencesProperty.setValue(customApiKeyPreferencesList.get(0));
    }

    @Override
    public void storeSettings() {
        preferencesService.storeImportSettingsPreferences(new ImportSettingsPreferences(
                generateKeyOnImportProperty.getValue()));

        preferencesService.storeDOIPreferences(new DOIPreferences(
                useCustomDOIProperty.getValue(),
                useCustomDOINameProperty.getValue().trim()));

        // API keys
        selectedCustomApiKeyPreferencesProperty.get().shouldUseCustomKey(useCustomApiKeyProperty.get());
        selectedCustomApiKeyPreferencesProperty.get().setCustomApiKey(customApiKeyTextProperty.get());
        for (CustomApiKeyPreferences apiKeyPreferences : customApiKeyPreferencesListProperty.get()) {
            if (apiKeyPreferences.getCustomApiKey().isEmpty()) {
                preferencesService.clearCustomApiKeyPreferences(apiKeyPreferences.getName());
            } else {
                preferencesService.storeCustomApiKeyPreferences(apiKeyPreferences);
            }
        }
    }

    public BooleanProperty generateKeyOnImportProperty() {
        return generateKeyOnImportProperty;
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }

    public ListProperty<CustomApiKeyPreferences> customApiKeyPrefsProperty() {
        return this.customApiKeyPreferencesListProperty;
    }

    public ObjectProperty<CustomApiKeyPreferences> selectedCustomApiKeyPreferencesProperty() {
        return this.selectedCustomApiKeyPreferencesProperty;
    }

    public BooleanProperty useCustomApiKeyProperty() {
        return this.useCustomApiKeyProperty;
    }

    public StringProperty customApiKeyText() {
        return this.customApiKeyTextProperty;
    }

    public void checkCustomApiKey() {
        final String apiKeyName = selectedCustomApiKeyPreferencesProperty.get().getName();
        final String testUrlWithoutApiKey = API_KEY_NAME_URL.get(apiKeyName);
        final String apiKey = customApiKeyTextProperty.get();

        boolean valid;
        if (!apiKey.isEmpty()) {
            URLDownload urlDownload;
            try {
                SSLSocketFactory defaultSslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
                HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
                URLDownload.bypassSSLVerification();

                urlDownload = new URLDownload(testUrlWithoutApiKey + apiKey);
                // The HEAD request cannot be used because its response is not 200 (maybe 404 or 596...).
                int statusCode = ((HttpURLConnection) urlDownload.getSource().openConnection()).getResponseCode();
                valid = statusCode >= 200 && statusCode < 300;

                URLDownload.setSSLVerification(defaultSslSocketFactory, defaultHostnameVerifier);
            } catch (IOException | kong.unirest.UnirestException e) {
                valid = false;
            }
        } else {
            valid = false;
        }

        if (valid) {
            dialogService.showInformationDialogAndWait(Localization.lang("Check %0 API Key Setting", apiKeyName), Localization.lang("Connection successful!"));
        } else {
            dialogService.showErrorDialogAndWait(Localization.lang("Check %0 API Key Setting", apiKeyName), Localization.lang("Connection failed!"));
        }
    }

}
