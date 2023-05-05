package org.jabref.gui.preferences.websearch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Optional;

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
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.CustomizableKeyFetcher;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.FetcherApiKey;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

public class WebSearchTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty generateKeyOnImportProperty = new SimpleBooleanProperty();
    private final BooleanProperty warnAboutDuplicatesOnImportProperty = new SimpleBooleanProperty();
    private final BooleanProperty shouldDownloadLinkedOnlineFiles = new SimpleBooleanProperty();

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    private final BooleanProperty grobidEnabledProperty = new SimpleBooleanProperty();
    private final StringProperty grobidURLProperty = new SimpleStringProperty("");

    private final ListProperty<FetcherApiKey> apiKeys = new SimpleListProperty<>();
    private final ObjectProperty<FetcherApiKey> selectedApiKeyProperty = new SimpleObjectProperty<>();

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final DOIPreferences doiPreferences;
    private final GrobidPreferences grobidPreferences;
    private final ImporterPreferences importerPreferences;
    private final FilePreferences filePreferences;

    public WebSearchTabViewModel(PreferencesService preferencesService, DialogService dialogService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.importerPreferences = preferencesService.getImporterPreferences();
        this.grobidPreferences = preferencesService.getGrobidPreferences();
        this.doiPreferences = preferencesService.getDOIPreferences();
        this.filePreferences = preferencesService.getFilePreferences();
    }

    @Override
    public void setValues() {
        generateKeyOnImportProperty.setValue(importerPreferences.isGenerateNewKeyOnImport());
        warnAboutDuplicatesOnImportProperty.setValue(importerPreferences.shouldWarnAboutDuplicatesOnImport());
        shouldDownloadLinkedOnlineFiles.setValue(filePreferences.shouldDownloadLinkedFiles());

        useCustomDOIProperty.setValue(doiPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(doiPreferences.getDefaultBaseURI());

        grobidEnabledProperty.setValue(grobidPreferences.isGrobidEnabled());
        grobidURLProperty.setValue(grobidPreferences.getGrobidURL());

        apiKeys.setValue(FXCollections.observableArrayList(preferencesService.getImporterPreferences().getApiKeys()));
    }

    @Override
    public void storeSettings() {
        importerPreferences.setGenerateNewKeyOnImport(generateKeyOnImportProperty.getValue());
        importerPreferences.setWarnAboutDuplicatesOnImport(warnAboutDuplicatesOnImportProperty.getValue());
        filePreferences.setDownloadLinkedFiles(shouldDownloadLinkedOnlineFiles.getValue());

        grobidPreferences.setGrobidEnabled(grobidEnabledProperty.getValue());
        grobidPreferences.setGrobidOptOut(grobidPreferences.isGrobidOptOut());
        grobidPreferences.setGrobidURL(grobidURLProperty.getValue());

        doiPreferences.setUseCustom(useCustomDOIProperty.get());
        doiPreferences.setDefaultBaseURI(useCustomDOINameProperty.getValue().trim());

        preferencesService.getImporterPreferences().getApiKeys().clear();
        preferencesService.getImporterPreferences().getApiKeys().addAll(apiKeys);
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

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabledProperty;
    }

    public StringProperty grobidURLProperty() {
        return grobidURLProperty;
    }

    public ListProperty<FetcherApiKey> fetcherApiKeys() {
        return apiKeys;
    }

    public ObjectProperty<FetcherApiKey> selectedApiKeyProperty() {
        return selectedApiKeyProperty;
    }

    public BooleanProperty warnAboutDuplicatesOnImportProperty() {
        return warnAboutDuplicatesOnImportProperty;
    }

    public BooleanProperty shouldDownloadLinkedOnlineFiles() {
        return shouldDownloadLinkedOnlineFiles;
    }

    public void checkCustomApiKey() {
        final String apiKeyName = selectedApiKeyProperty.get().getName();

        final Optional<CustomizableKeyFetcher> fetcherOpt =
                WebFetchers.getCustomizableKeyFetchers(
                                   preferencesService.getImportFormatPreferences(),
                                   preferencesService.getImporterPreferences())
                           .stream()
                           .filter(fetcher -> fetcher.getName().equals(apiKeyName))
                           .findFirst();

        if (fetcherOpt.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("Check %0 API Key Setting", apiKeyName),
                    Localization.lang("Fetcher unknown!"));
            return;
        }

        final String testUrlWithoutApiKey = fetcherOpt.get().getTestUrl();
        if (testUrlWithoutApiKey == null) {
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Check %0 API Key Setting", apiKeyName),
                    Localization.lang("Fetcher cannot be tested!"));
            return;
        }

        final String apiKey = selectedApiKeyProperty.get().getKey();

        boolean keyValid;
        if (!apiKey.isEmpty()) {
            URLDownload urlDownload;
            try {
                SSLSocketFactory defaultSslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
                HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

                urlDownload = new URLDownload(testUrlWithoutApiKey + apiKey);
                // The HEAD request cannot be used because its response is not 200 (maybe 404 or 596...).
                int statusCode = ((HttpURLConnection) urlDownload.getSource().openConnection()).getResponseCode();
                keyValid = (statusCode >= 200) && (statusCode < 300);

                URLDownload.setSSLVerification(defaultSslSocketFactory, defaultHostnameVerifier);
            } catch (IOException | kong.unirest.UnirestException e) {
                keyValid = false;
            }
        } else {
            keyValid = false;
        }

        if (keyValid) {
            dialogService.showInformationDialogAndWait(Localization.lang("Check %0 API Key Setting", apiKeyName), Localization.lang("Connection successful!"));
        } else {
            dialogService.showErrorDialogAndWait(Localization.lang("Check %0 API Key Setting", apiKeyName), Localization.lang("Connection failed!"));
        }
    }
}
