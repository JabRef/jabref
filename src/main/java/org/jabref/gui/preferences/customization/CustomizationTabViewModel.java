package org.jabref.gui.preferences.customization;

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

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.preferences.CustomApiKeyPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.preferences.PreferencesService;

public class CustomizationTabViewModel implements PreferenceTabViewModel {

    private final static Map<String, String> API_KEY_NAME_URL = new TreeMap<>();
    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    private final ListProperty<CustomApiKeyPreferences> customApiKeyPrefsListProperty = new SimpleListProperty<>();
    private final ObjectProperty<CustomApiKeyPreferences> selectedCustomApiKeyPrefProperty = new SimpleObjectProperty<>();
    private final BooleanProperty useCustomApiKeyProperty = new SimpleBooleanProperty();
    private final StringProperty customApiKeyTextProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final DOIPreferences initialDOIPreferences;

    public CustomizationTabViewModel(DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.initialDOIPreferences = preferencesService.getDOIPreferences();
        initApiKeyNameUrl();
    }

    private void initApiKeyNameUrl() {
        // Springer query using the parameter 'q=doi:10.1007/s11276-008-0131-4s=1' will respond faster
        API_KEY_NAME_URL.put("Springer", "https://api.springernature.com/meta/v1/json?q=doi:10.1007/s11276-008-0131-4s=1&p=1&api_key=");
        API_KEY_NAME_URL.put("IEEEXplore", "https://ieeexploreapi.ieee.org/api/v1/search/articles?max_records=0&apikey=");
    }

    /**
     * Gets a list of Fetcher names which in the custom Tab
     *
     * @return ArrayList containing Fetcher names
     */
    public ArrayList<String> getCustomApiKeyFetchers() {
        return new ArrayList<>(API_KEY_NAME_URL.keySet());
    }

    /**
     * Reads user settings when the dialog is opened.
     */
    @Override
    public void setValues() {
        useCustomDOIProperty.setValue(initialDOIPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(initialDOIPreferences.getDefaultBaseURI());

        // Initialize API KEY preferences and property
        ArrayList<CustomApiKeyPreferences> customApiKeyPreferencesList = new ArrayList<>();
        for (String name : API_KEY_NAME_URL.keySet()) {
            customApiKeyPreferencesList.add(preferencesService.getCustomApiKeyPreferences(name));
        }
        customApiKeyPrefsListProperty.setValue(FXCollections.observableArrayList(customApiKeyPreferencesList));
        selectedCustomApiKeyPrefProperty.setValue(customApiKeyPreferencesList.get(0));
    }

    /**
     * Stores user settings when the user presses OK in the Preferences dialog.
     */
    @Override
    public void storeSettings() {
        preferencesService.storeDOIPreferences(new DOIPreferences(
                useCustomDOIProperty.getValue(),
                useCustomDOINameProperty.getValue().trim()));
        selectedCustomApiKeyPrefProperty.get().useCustom(useCustomApiKeyProperty.get());
        selectedCustomApiKeyPrefProperty.get().setCustomApiKey(customApiKeyTextProperty.get());
        for (CustomApiKeyPreferences apiKeyPreferences : customApiKeyPrefsListProperty.get()) {
            if (apiKeyPreferences.getCustomApiKey().isEmpty()) {
                preferencesService.clearCustomApiKeyPreferences(apiKeyPreferences.getName());
            } else {
                preferencesService.storeCustomApiKeyPreferences(apiKeyPreferences);
            }
        }
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }

    public ListProperty<CustomApiKeyPreferences> customApiKeyPrefsProperty() {
        return this.customApiKeyPrefsListProperty;
    }

    public ObjectProperty<CustomApiKeyPreferences> selectedCustomApiKeyPrefProperty() {
        return this.selectedCustomApiKeyPrefProperty;
    }

    public BooleanProperty useCustomApiKeyProperty() {
        return this.useCustomApiKeyProperty;
    }

    public StringProperty customApiKeyText() {
        return this.customApiKeyTextProperty;
    }

    public void checkCustomApiKey() {
        final String apiKeyName = selectedCustomApiKeyPrefProperty.get().getName();
        final String testUrlWithoutApiKey = API_KEY_NAME_URL.get(apiKeyName);
        final String apiKey = customApiKeyTextProperty.get();

        final String connectionSuccessText = Localization.lang("Connection successful!");
        final String connectionFailedText = Localization.lang("Connection failed!");
        final String dialogTitle = Localization.lang("Check %0 API Key Setting", apiKeyName);

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
            dialogService.showInformationDialogAndWait(dialogTitle, connectionSuccessText);
        } else {
            dialogService.showErrorDialogAndWait(dialogTitle, connectionFailedText);
        }
    }

}
