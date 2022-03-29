package org.jabref.gui.preferences.importexport;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
import org.jabref.gui.commonfxcontrols.SortCriterionViewModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.FetcherApiKey;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.preferences.PreferencesService;

public class ImportExportTabViewModel implements PreferenceTabViewModel {

    private final static Map<String, String> API_KEY_NAME_URL = new TreeMap<>();

    private final BooleanProperty generateKeyOnImportProperty = new SimpleBooleanProperty();

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    // SaveOrderConfigPanel
    private final BooleanProperty exportInOriginalProperty = new SimpleBooleanProperty();
    private final BooleanProperty exportInTableOrderProperty = new SimpleBooleanProperty();
    private final BooleanProperty exportInSpecifiedOrderProperty = new SimpleBooleanProperty();
    private final ListProperty<Field> sortableFieldsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<SortCriterionViewModel> sortCriteriaProperty = new SimpleListProperty<>(FXCollections.observableArrayList(new ArrayList<>()));

    private final BooleanProperty grobidEnabledProperty = new SimpleBooleanProperty();
    private final StringProperty grobidURLProperty = new SimpleStringProperty("");

    private final ListProperty<FetcherApiKey> keysProperty = new SimpleListProperty<>();
    private final ObjectProperty<FetcherApiKey> selectedCustomApiKeyPreferencesProperty = new SimpleObjectProperty<>();
    private final BooleanProperty useCustomApiKeyProperty = new SimpleBooleanProperty();
    private final StringProperty customApiKeyTextProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final DOIPreferences doiPreferences;
    private final ImporterPreferences importerPreferences;
    private final SaveOrderConfig initialExportOrder;

    public ImportExportTabViewModel(PreferencesService preferencesService, DOIPreferences doiPreferences, DialogService dialogService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.importerPreferences = preferencesService.getImporterPreferences();
        this.doiPreferences = doiPreferences;
        this.initialExportOrder = preferencesService.getExportSaveOrder();
    }

    public static void registerApiKeyCustom(String key, String testUrlWithoutApiKey) {
        API_KEY_NAME_URL.put(key, testUrlWithoutApiKey);
    }

    public ArrayList<String> getCustomApiKeyFetchers() {
        return new ArrayList<>(API_KEY_NAME_URL.keySet());
    }

    @Override
    public void setValues() {
        generateKeyOnImportProperty.setValue(importerPreferences.isGenerateNewKeyOnImport());
        useCustomDOIProperty.setValue(doiPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(doiPreferences.getDefaultBaseURI());

        switch (initialExportOrder.getOrderType()) {
            case SPECIFIED -> exportInSpecifiedOrderProperty.setValue(true);
            case ORIGINAL -> exportInOriginalProperty.setValue(true);
            case TABLE -> exportInTableOrderProperty.setValue(true);
        }

        List<Field> fieldNames = new ArrayList<>(FieldFactory.getCommonFields());
        fieldNames.sort(Comparator.comparing(Field::getDisplayName));

        sortableFieldsProperty.addAll(fieldNames);
        sortCriteriaProperty.addAll(initialExportOrder.getSortCriteria().stream()
                                                      .map(SortCriterionViewModel::new)
                                                      .toList());

        grobidEnabledProperty.setValue(importerPreferences.isGrobidEnabled());
        grobidURLProperty.setValue(importerPreferences.getGrobidURL());

        // API keys
        for (String name : API_KEY_NAME_URL.keySet()) {
            keysProperty.add(preferencesService.getCustomApiKeyPreferences(name));
        }
    }

    @Override
    public void storeSettings() {
        importerPreferences.setGenerateNewKeyOnImport(generateKeyOnImportProperty.getValue());
        importerPreferences.setGrobidEnabled(grobidEnabledProperty.getValue());
        importerPreferences.setGrobidOptOut(importerPreferences.isGrobidOptOut());
        importerPreferences.setGrobidURL(grobidURLProperty.getValue());

        doiPreferences.setUseCustom(useCustomDOIProperty.get());
        doiPreferences.setDefaultBaseURI(useCustomDOINameProperty.getValue().trim());

        SaveOrderConfig newSaveOrderConfig = new SaveOrderConfig(
                SaveOrderConfig.OrderType.fromBooleans(exportInSpecifiedOrderProperty.getValue(), exportInTableOrderProperty.getValue()),
                sortCriteriaProperty.stream().map(SortCriterionViewModel::getCriterion).toList());
        preferencesService.storeExportSaveOrder(newSaveOrderConfig);

        // API keys
        selectedCustomApiKeyPreferencesProperty.get().shouldUseCustomKey(useCustomApiKeyProperty.get());
        selectedCustomApiKeyPreferencesProperty.get().setCustomApiKey(customApiKeyTextProperty.get());
        for (FetcherApiKey apiKeyPreferences : keysProperty.get()) {
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

    // SaveOrderConfigPanel

    public BooleanProperty saveInOriginalProperty() {
        return exportInOriginalProperty;
    }

    public BooleanProperty saveInTableOrderProperty() {
        return exportInTableOrderProperty;
    }

    public BooleanProperty saveInSpecifiedOrderProperty() {
        return exportInSpecifiedOrderProperty;
    }

    public ListProperty<Field> sortableFieldsProperty() {
        return sortableFieldsProperty;
    }

    public ListProperty<SortCriterionViewModel> sortCriteriaProperty() {
        return sortCriteriaProperty;
    }

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabledProperty;
    }

    public StringProperty grobidURLProperty() {
        return grobidURLProperty;
    }

    public ListProperty<FetcherApiKey> customApiKeyPrefsProperty() {
        return this.keysProperty;
    }

    public ObjectProperty<FetcherApiKey> selectedCustomApiKeyPreferencesProperty() {
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
