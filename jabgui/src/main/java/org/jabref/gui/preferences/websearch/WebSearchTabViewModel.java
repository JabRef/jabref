package org.jabref.gui.preferences.websearch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.slr.StudyCatalogItem;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.CompositeSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.CustomizableKeyFetcher;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.FetcherApiKey;

import kong.unirest.core.UnirestException;

public class WebSearchTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty enableWebSearchProperty = new SimpleBooleanProperty();
    private final BooleanProperty warnAboutDuplicatesOnImportProperty = new SimpleBooleanProperty();
    private final BooleanProperty shouldDownloadLinkedOnlineFiles = new SimpleBooleanProperty();
    private final BooleanProperty shouldkeepDownloadUrl = new SimpleBooleanProperty();

    private final ListProperty<PlainCitationParserChoice> plainCitationParsers =
            new SimpleListProperty<>(FXCollections.observableArrayList(PlainCitationParserChoice.values()));
    private final ObjectProperty<PlainCitationParserChoice> defaultPlainCitationParser = new SimpleObjectProperty<>();

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    private final ObservableList<StudyCatalogItem> catalogs = FXCollections.observableArrayList();
    private final BooleanProperty grobidEnabledProperty = new SimpleBooleanProperty();
    private final StringProperty grobidURLProperty = new SimpleStringProperty("");

    private final ObservableList<FetcherApiKey> apiKeys = FXCollections.observableArrayList();
    private final ObjectProperty<FetcherApiKey> selectedApiKeyProperty = new SimpleObjectProperty<>();
    private final BooleanProperty apikeyPersistProperty = new SimpleBooleanProperty();
    private final BooleanProperty apikeyPersistAvailableProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final DOIPreferences doiPreferences;
    private final GrobidPreferences grobidPreferences;
    private final ImporterPreferences importerPreferences;
    private final FilePreferences filePreferences;
    private final ImportFormatPreferences importFormatPreferences;

    private final ReadOnlyBooleanProperty refAiEnabled;

    public WebSearchTabViewModel(CliPreferences preferences, DialogService dialogService, ReadOnlyBooleanProperty refAiEnabled) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.importerPreferences = preferences.getImporterPreferences();
        this.grobidPreferences = preferences.getGrobidPreferences();
        this.doiPreferences = preferences.getDOIPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.importFormatPreferences = preferences.getImportFormatPreferences();

        this.refAiEnabled = refAiEnabled;

        setupPlainCitationParsers(preferences);
    }

    private void setupPlainCitationParsers(CliPreferences preferences) {
        if (!refAiEnabled.get()) {
            plainCitationParsers.remove(PlainCitationParserChoice.LLM);
        }

        refAiEnabled.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                plainCitationParsers.add(PlainCitationParserChoice.LLM);
            } else {
                PlainCitationParserChoice oldChoice = defaultPlainCitationParser.get();

                plainCitationParsers.remove(PlainCitationParserChoice.LLM);

                if (oldChoice == PlainCitationParserChoice.LLM) {
                    defaultPlainCitationParser.set(plainCitationParsers.getFirst());
                }
            }
        });

        if (!grobidEnabledProperty().get()) {
            plainCitationParsers.remove(PlainCitationParserChoice.GROBID);
        }

        grobidEnabledProperty.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                plainCitationParsers.add(PlainCitationParserChoice.GROBID);
            } else {
                PlainCitationParserChoice oldChoice = defaultPlainCitationParser.get();

                plainCitationParsers.remove(PlainCitationParserChoice.GROBID);

                if (oldChoice == PlainCitationParserChoice.GROBID) {
                    defaultPlainCitationParser.set(plainCitationParsers.getFirst());
                }
            }
        });
    }

    @Override
    public void setValues() {
        enableWebSearchProperty.setValue(importerPreferences.areImporterEnabled());
        warnAboutDuplicatesOnImportProperty.setValue(importerPreferences.shouldWarnAboutDuplicatesOnImport());
        shouldDownloadLinkedOnlineFiles.setValue(filePreferences.shouldDownloadLinkedFiles());
        shouldkeepDownloadUrl.setValue(filePreferences.shouldKeepDownloadUrl());
        defaultPlainCitationParser.setValue(importerPreferences.getDefaultPlainCitationParser());

        useCustomDOIProperty.setValue(doiPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(doiPreferences.getDefaultBaseURI());

        grobidEnabledProperty.setValue(grobidPreferences.isGrobidEnabled());
        grobidURLProperty.setValue(grobidPreferences.getGrobidURL());

        apiKeys.setAll(preferences.getImporterPreferences().getApiKeys().stream()
                                  .map(apiKey -> new FetcherApiKey(apiKey.getName(), apiKey.shouldUse(), apiKey.getKey()))
                                  .toList());

        apikeyPersistAvailableProperty.setValue(OS.isKeyringAvailable());
        apikeyPersistProperty.setValue(preferences.getImporterPreferences().shouldPersistCustomKeys());
        catalogs.addAll(WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                   .stream()
                                   .map(SearchBasedFetcher::getName)
                                   .filter(name -> !CompositeSearchBasedFetcher.FETCHER_NAME.equals(name))
                                   .map(name -> {
                                       boolean enabled = importerPreferences.getCatalogs().contains(name);
                                       return new StudyCatalogItem(name, enabled);
                                   })
                                   .toList());
    }

    @Override
    public void storeSettings() {
        importerPreferences.setImporterEnabled(enableWebSearchProperty.getValue());
        importerPreferences.setWarnAboutDuplicatesOnImport(warnAboutDuplicatesOnImportProperty.getValue());
        filePreferences.setDownloadLinkedFiles(shouldDownloadLinkedOnlineFiles.getValue());
        filePreferences.setKeepDownloadUrl(shouldkeepDownloadUrl.getValue());
        importerPreferences.setDefaultPlainCitationParser(defaultPlainCitationParser.getValue());
        grobidPreferences.setGrobidEnabled(grobidEnabledProperty.getValue());
        grobidPreferences.setGrobidUseAsked(grobidPreferences.isGrobidUseAsked());
        grobidPreferences.setGrobidURL(grobidURLProperty.getValue());
        doiPreferences.setUseCustom(useCustomDOIProperty.get());
        doiPreferences.setDefaultBaseURI(useCustomDOINameProperty.getValue().trim());
        importerPreferences.setCatalogs(
                FXCollections.observableList(catalogs.stream()
                                                     .filter(StudyCatalogItem::isEnabled)
                                                     .map(StudyCatalogItem::getName)
                                                     .collect(Collectors.toList())));
        importerPreferences.setPersistCustomKeys(apikeyPersistProperty.get());
        preferences.getImporterPreferences().getApiKeys().clear();
        if (apikeyPersistAvailableProperty.get()) {
            preferences.getImporterPreferences().getApiKeys().addAll(apiKeys);
        }
    }

    public BooleanProperty enableWebSearchProperty() {
        return enableWebSearchProperty;
    }

    public ListProperty<PlainCitationParserChoice> plainCitationParsers() {
        return plainCitationParsers;
    }

    public ObjectProperty<PlainCitationParserChoice> defaultPlainCitationParserProperty() {
        return defaultPlainCitationParser;
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }

    public ObservableList<StudyCatalogItem> getCatalogs() {
        return catalogs;
    }

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabledProperty;
    }

    public StringProperty grobidURLProperty() {
        return grobidURLProperty;
    }

    public ObservableList<FetcherApiKey> fetcherApiKeys() {
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

    public BooleanProperty shouldKeepDownloadUrl() {
        return shouldkeepDownloadUrl;
    }

    public ReadOnlyBooleanProperty apiKeyPersistAvailable() {
        return apikeyPersistAvailableProperty;
    }

    public BooleanProperty getApikeyPersistProperty() {
        return apikeyPersistProperty;
    }

    public void checkCustomApiKey() {
        final String apiKeyName = selectedApiKeyProperty.get().getName();

        final Optional<CustomizableKeyFetcher> fetcherOpt =
                WebFetchers.getCustomizableKeyFetchers(
                                   preferences.getImportFormatPreferences(),
                                   preferences.getImporterPreferences())
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
                urlDownload = new URLDownload(testUrlWithoutApiKey + apiKey);
                // The HEAD request cannot be used because its response is not 200 (maybe 404 or 596...).
                int statusCode = ((HttpURLConnection) urlDownload.getSource().openConnection()).getResponseCode();
                keyValid = (statusCode >= 200) && (statusCode < 300);
            } catch (IOException | UnirestException e) {
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

    @Override
    public boolean validateSettings() {
        return getCatalogs().stream().anyMatch(StudyCatalogItem::isEnabled);
    }
}
