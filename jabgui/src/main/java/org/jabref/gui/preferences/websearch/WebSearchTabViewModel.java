package org.jabref.gui.preferences.websearch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetcher;
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
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import kong.unirest.core.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSearchTabViewModel implements PreferenceTabViewModel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSearchTabViewModel.class);
    private final BooleanProperty enableWebSearchProperty = new SimpleBooleanProperty();
    private final BooleanProperty warnAboutDuplicatesOnImportProperty = new SimpleBooleanProperty();
    private final BooleanProperty shouldDownloadLinkedOnlineFiles = new SimpleBooleanProperty();
    private final BooleanProperty shouldkeepDownloadUrl = new SimpleBooleanProperty();

    private final ListProperty<PlainCitationParserChoice> plainCitationParsers =
            new SimpleListProperty<>(FXCollections.observableArrayList(PlainCitationParserChoice.values()));
    private final ObjectProperty<PlainCitationParserChoice> defaultPlainCitationParser = new SimpleObjectProperty<>();

    private final IntegerProperty citationsRelationStoreTTL = new SimpleIntegerProperty();

    private final BooleanProperty addImportedEntries = new SimpleBooleanProperty();
    private final StringProperty addImportedEntriesGroupName = new SimpleStringProperty("");

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    private final ObservableList<FetcherViewModel> fetchers = FXCollections.observableArrayList();
    private final BooleanProperty grobidEnabledProperty = new SimpleBooleanProperty();
    private final StringProperty grobidURLProperty = new SimpleStringProperty("");

    private final BooleanProperty preferInspireTexkeysProperty = new SimpleBooleanProperty();

    private final BooleanProperty apikeyPersistProperty = new SimpleBooleanProperty();
    private final BooleanProperty apikeyPersistAvailableProperty = new SimpleBooleanProperty();

    private final CliPreferences preferences;
    private final DOIPreferences doiPreferences;
    private final GrobidPreferences grobidPreferences;
    private final ImporterPreferences importerPreferences;
    private final FilePreferences filePreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final LibraryPreferences libraryPreferences;
    private final TaskExecutor taskExecutor;

    private final ReadOnlyBooleanProperty refAiEnabled;

    public WebSearchTabViewModel(CliPreferences preferences, ReadOnlyBooleanProperty refAiEnabled, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.importerPreferences = preferences.getImporterPreferences();
        this.grobidPreferences = preferences.getGrobidPreferences();
        this.doiPreferences = preferences.getDOIPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.importFormatPreferences = preferences.getImportFormatPreferences();
        this.libraryPreferences = preferences.getLibraryPreferences();
        this.taskExecutor = taskExecutor;

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
        LOGGER.debug("Setting values for WebSearchTabViewModel");
        
        enableWebSearchProperty.setValue(importerPreferences.areImporterEnabled());
        warnAboutDuplicatesOnImportProperty.setValue(importerPreferences.shouldWarnAboutDuplicatesOnImport());
        shouldDownloadLinkedOnlineFiles.setValue(filePreferences.shouldDownloadLinkedFiles());
        shouldkeepDownloadUrl.setValue(filePreferences.shouldKeepDownloadUrl());
        addImportedEntries.setValue(libraryPreferences.isAddImportedEntriesEnabled());
        addImportedEntriesGroupName.setValue(libraryPreferences.getAddImportedEntriesGroupName());
        defaultPlainCitationParser.setValue(importerPreferences.getDefaultPlainCitationParser());
        citationsRelationStoreTTL.setValue(importerPreferences.getCitationsRelationsStoreTTL());
        
        LOGGER.debug("Web search enabled: {}, Duplicate warning: {}, Download linked files: {}", 
                importerPreferences.areImporterEnabled(), 
                importerPreferences.shouldWarnAboutDuplicatesOnImport(),
                filePreferences.shouldDownloadLinkedFiles());

        useCustomDOIProperty.setValue(doiPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(doiPreferences.getDefaultBaseURI());

        grobidEnabledProperty.setValue(grobidPreferences.isGrobidEnabled());
        grobidURLProperty.setValue(grobidPreferences.getGrobidURL());

        preferInspireTexkeysProperty.setValue(preferences.getImporterPreferences().isPreferInspireTexkeys());

        Set<FetcherApiKey> savedApiKeys = preferences.getImporterPreferences().getApiKeys();
        Set<String> enabledCatalogs = new HashSet<>(importerPreferences.getCatalogs());

        List<SearchBasedFetcher> allFetchers = WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                                          .stream().sorted(Comparator.comparing(WebFetcher::getName)).toList();

        Set<CustomizableKeyFetcher> customizableKeyFetchers = WebFetchers.getCustomizableKeyFetchers(importFormatPreferences, importerPreferences);
        Set<String> customizableFetcherNames = customizableKeyFetchers.stream().map(WebFetcher::getName).collect(Collectors.toSet());

        fetchers.clear();
        for (SearchBasedFetcher fetcher : allFetchers) {
            if (CompositeSearchBasedFetcher.FETCHER_NAME.equals(fetcher.getName())) {
                continue;
            }
            boolean isEnabled = enabledCatalogs.contains(fetcher.getName());
            boolean isCustomizable = customizableFetcherNames.contains(fetcher.getName());
            FetcherViewModel fetcherViewModel = new FetcherViewModel(fetcher, isEnabled, isCustomizable);
            if (isCustomizable) {
                savedApiKeys.stream()
                            .filter(apiKey -> apiKey.getName().equals(fetcher.getName()))
                            .findFirst()
                            .ifPresent(apiKey -> {
                                fetcherViewModel.apiKeyProperty().set(apiKey.getKey());
                                fetcherViewModel.useCustomApiKeyProperty().set(apiKey.shouldUse());
                            });
            }
            fetchers.add(fetcherViewModel);
        }

        apikeyPersistAvailableProperty.setValue(OS.isKeyringAvailable());
        apikeyPersistProperty.setValue(preferences.getImporterPreferences().shouldPersistCustomKeys());
    }

    @Override
    public void storeSettings() {
        LOGGER.debug("Storing WebSearchTabViewModel settings");
        
        importerPreferences.setImporterEnabled(enableWebSearchProperty.getValue());
        importerPreferences.setWarnAboutDuplicatesOnImport(warnAboutDuplicatesOnImportProperty.getValue());
        filePreferences.setDownloadLinkedFiles(shouldDownloadLinkedOnlineFiles.getValue());
        filePreferences.setKeepDownloadUrl(shouldkeepDownloadUrl.getValue());
        libraryPreferences.setAddImportedEntries(addImportedEntries.getValue());
        
        LOGGER.info("Web search settings updated - Enabled: {}, Duplicate warning: {}, Download linked files: {}", 
                enableWebSearchProperty.getValue(),
                warnAboutDuplicatesOnImportProperty.getValue(),
                shouldDownloadLinkedOnlineFiles.getValue());
        if (addImportedEntriesGroupName.getValue().isEmpty() || addImportedEntriesGroupName.getValue().startsWith(" ")) {
            libraryPreferences.setAddImportedEntriesGroupName(Localization.lang("Imported entries"));
        } else {
            libraryPreferences.setAddImportedEntriesGroupName(addImportedEntriesGroupName.getValue());
        }
        importerPreferences.setDefaultPlainCitationParser(defaultPlainCitationParser.getValue());
        importerPreferences.setCitationsRelationsStoreTTL(citationsRelationStoreTTL.getValue());

        grobidPreferences.setGrobidEnabled(grobidEnabledProperty.getValue());
        grobidPreferences.setGrobidUseAsked(grobidPreferences.isGrobidUseAsked());
        grobidPreferences.setGrobidURL(grobidURLProperty.getValue());
        doiPreferences.setUseCustom(useCustomDOIProperty.get());
        doiPreferences.setDefaultBaseURI(useCustomDOINameProperty.getValue().trim());

        importerPreferences.setPreferInspireTexkeys(preferInspireTexkeysProperty.getValue());

        importerPreferences.setCatalogs(
                fetchers.stream()
                        .filter(FetcherViewModel::isEnabled)
                        .map(FetcherViewModel::getName)
                        .toList());

        List<FetcherApiKey> apiKeysToStore = fetchers.stream()
                                                     .filter(FetcherViewModel::isCustomizable)
                                                     .map(fetcherViewModel -> new FetcherApiKey(fetcherViewModel.getName(), fetcherViewModel.shouldUseCustomApiKey(), fetcherViewModel.getApiKey()))
                                                     .toList();

        importerPreferences.setPersistCustomKeys(apikeyPersistProperty.get());
        preferences.getImporterPreferences().getApiKeys().clear();
        if (apikeyPersistAvailableProperty.get()) {
            preferences.getImporterPreferences().getApiKeys().addAll(apiKeysToStore);
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

    public BooleanProperty getAddImportedEntries() {
        return addImportedEntries;
    }

    public StringProperty getAddImportedEntriesGroupName() {
        return addImportedEntriesGroupName;
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }

    public ObservableList<FetcherViewModel> getFetchers() {
        return fetchers;
    }

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabledProperty;
    }

    public StringProperty grobidURLProperty() {
        return grobidURLProperty;
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

    public IntegerProperty citationsRelationsStoreTTLProperty() {
        return citationsRelationStoreTTL;
    }

    public BooleanProperty preferInspireTexkeysProperty() {
        return preferInspireTexkeysProperty;
    }

    public void checkApiKey(FetcherViewModel fetcherViewModel, String apiKey, Consumer<Boolean> onFinished) {
        LOGGER.debug("Checking API key for fetcher: {}", fetcherViewModel.getName());
        
        Callable<Boolean> tester = () -> {
            WebFetcher webFetcher = fetcherViewModel.getFetcher();

            if (!(webFetcher instanceof CustomizableKeyFetcher fetcher)) {
                LOGGER.warn("Fetcher {} is not a CustomizableKeyFetcher", fetcherViewModel.getName());
                return false;
            }

            String testUrlWithoutApiKey = fetcher.getTestUrl();
            if (testUrlWithoutApiKey == null) {
                LOGGER.warn("No test URL available for fetcher: {}", fetcherViewModel.getName());
                return false;
            }

            if (apiKey.isEmpty()) {
                LOGGER.warn("Empty API key provided for fetcher: {}", fetcherViewModel.getName());
                return false;
            }

            try {
                URLDownload urlDownload = new URLDownload(testUrlWithoutApiKey + apiKey);
                // The HEAD request cannot be used because its response is not 200 (maybe 404 or 596...).
                int statusCode = ((HttpURLConnection) urlDownload.getSource().openConnection()).getResponseCode();
                boolean isValid = (statusCode >= 200) && (statusCode < 300);
                LOGGER.debug("API key validation for {} returned status code: {}, valid: {}", 
                        fetcherViewModel.getName(), statusCode, isValid);
                return isValid;
            } catch (IOException | UnirestException e) {
                LOGGER.warn("Error validating API key for fetcher {}: {}", fetcherViewModel.getName(), e.getMessage());
                return false;
            }
        };
        BackgroundTask.wrap(tester)
                      .onSuccess(result -> {
                          LOGGER.info("API key validation completed for {}: {}", fetcherViewModel.getName(), result);
                          onFinished.accept(result);
                      })
                      .onFailure(exception -> {
                          LOGGER.error("API key validation failed for {}: {}", fetcherViewModel.getName(), exception.getMessage());
                          onFinished.accept(false);
                      })
                      .executeWith(taskExecutor);
    }

    @Override
    public boolean validateSettings() {
        return getFetchers().stream().anyMatch(FetcherViewModel::isEnabled);
    }

    public static class FetcherViewModel {
        private final StringProperty name = new SimpleStringProperty();
        private final BooleanProperty enabled = new SimpleBooleanProperty();
        private final BooleanProperty customizable = new SimpleBooleanProperty();
        private final StringProperty apiKey = new SimpleStringProperty("");
        private final BooleanProperty useCustomApiKey = new SimpleBooleanProperty(false);
        private final WebFetcher fetcher;

        public FetcherViewModel(WebFetcher fetcher, boolean enabled, boolean customizable) {
            this.name.set(fetcher.getName());
            this.fetcher = fetcher;
            this.enabled.set(enabled);
            this.customizable.set(customizable);
        }

        public String getName() {
            return name.get();
        }

        public StringProperty nameProperty() {
            return name;
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public BooleanProperty enabledProperty() {
            return enabled;
        }

        public boolean isCustomizable() {
            return customizable.get();
        }

        public BooleanProperty customizableProperty() {
            return customizable;
        }

        public String getApiKey() {
            return apiKey.get();
        }

        public StringProperty apiKeyProperty() {
            return apiKey;
        }

        public boolean shouldUseCustomApiKey() {
            return useCustomApiKey.get();
        }

        public BooleanProperty useCustomApiKeyProperty() {
            return useCustomApiKey;
        }

        public WebFetcher getFetcher() {
            return fetcher;
        }
    }
}
