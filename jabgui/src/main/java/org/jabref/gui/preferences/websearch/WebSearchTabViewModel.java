package org.jabref.gui.preferences.websearch;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

public class WebSearchTabViewModel implements PreferenceTabViewModel {
    private final BooleanProperty enableWebSearchProperty = new SimpleBooleanProperty();
    private final BooleanProperty warnAboutDuplicatesOnImportProperty = new SimpleBooleanProperty();
    private final BooleanProperty shouldDownloadLinkedOnlineFiles = new SimpleBooleanProperty();
    private final BooleanProperty shouldKeepDownloadUrl = new SimpleBooleanProperty();

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

    private final BooleanProperty apikeyPersistProperty = new SimpleBooleanProperty();
    private final BooleanProperty apikeyPersistAvailableProperty = new SimpleBooleanProperty();

    private final ObservableList<SearchEngineItem> searchEngines = FXCollections.observableArrayList();

    private final DOIPreferences doiPreferences;
    private final GrobidPreferences grobidPreferences;
    private final ImporterPreferences importerPreferences;
    private final FilePreferences filePreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final LibraryPreferences libraryPreferences;
    private final TaskExecutor taskExecutor;

    private final ReadOnlyBooleanProperty refAiEnabled;

    public WebSearchTabViewModel(CliPreferences preferences, ReadOnlyBooleanProperty refAiEnabled, TaskExecutor taskExecutor) {
        this.importerPreferences = preferences.getImporterPreferences();
        this.grobidPreferences = preferences.getGrobidPreferences();
        this.doiPreferences = preferences.getDOIPreferences();
        this.filePreferences = preferences.getFilePreferences();
        this.importFormatPreferences = preferences.getImportFormatPreferences();
        this.libraryPreferences = preferences.getLibraryPreferences();
        this.taskExecutor = taskExecutor;

        this.refAiEnabled = refAiEnabled;

        setupPlainCitationParsers();
        setupSearchEngines();
    }

    private void setupPlainCitationParsers() {
        if (!refAiEnabled.get()) {
            plainCitationParsers.remove(PlainCitationParserChoice.LLM);
        }

        refAiEnabled.addListener((_, _, newValue) -> {
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

        grobidEnabledProperty.addListener((_, _, newValue) -> {
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

    private void setupSearchEngines() {
        // add default search engines
        searchEngines.addAll(
                new SearchEngineItem("Google Scholar", "https://scholar.google.com/scholar?q={title}"),
                new SearchEngineItem("Semantic Scholar", "https://www.semanticscholar.org/search?q={title}"),
                new SearchEngineItem("Short Science", "https://www.shortscience.org/internalsearch?q={title}")
        );
    }

    @Override
    public void setValues() {
        enableWebSearchProperty.setValue(importerPreferences.areImporterEnabled());
        warnAboutDuplicatesOnImportProperty.setValue(importerPreferences.shouldWarnAboutDuplicatesOnImport());
        shouldDownloadLinkedOnlineFiles.setValue(filePreferences.shouldDownloadLinkedFiles());
        shouldKeepDownloadUrl.setValue(filePreferences.shouldKeepDownloadUrl());
        addImportedEntries.setValue(libraryPreferences.isAddImportedEntriesEnabled());
        addImportedEntriesGroupName.setValue(libraryPreferences.getAddImportedEntriesGroupName());
        defaultPlainCitationParser.setValue(importerPreferences.getDefaultPlainCitationParser());
        citationsRelationStoreTTL.setValue(importerPreferences.getCitationsRelationsStoreTTL());

        useCustomDOIProperty.setValue(doiPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(doiPreferences.getDefaultBaseURI());

        grobidEnabledProperty.setValue(grobidPreferences.isGrobidEnabled());
        grobidURLProperty.setValue(grobidPreferences.getGrobidURL());

        Set<FetcherApiKey> savedApiKeys = importerPreferences.getApiKeys();
        Set<String> enabledCatalogs = new HashSet<>(importerPreferences.getCatalogs());

        List<SearchBasedFetcher> allFetchers = WebFetchers.getSearchBasedFetchers(importFormatPreferences, importerPreferences)
                                                          .stream()
                                                          .sorted(Comparator.comparing(WebFetcher::getName))
                                                          .toList();

        // We need to use names, because [WebFetchers] creates new instances for the fetchers at each method - even if they are the same.
        Set<String> customizableKeyFetcherNames = WebFetchers.getCustomizableKeyFetchers(importFormatPreferences, importerPreferences).stream().map(WebFetcher::getName).collect(Collectors.toSet());

        fetchers.clear();
        for (SearchBasedFetcher fetcher : allFetchers) {
            if (CompositeSearchBasedFetcher.FETCHER_NAME.equals(fetcher.getName())) {
                continue;
            }
            boolean isEnabled = enabledCatalogs.contains(fetcher.getName());
            boolean keyIsCustomizable = customizableKeyFetcherNames.contains(fetcher.getName());
            FetcherViewModel fetcherViewModel = new FetcherViewModel(fetcher, isEnabled, keyIsCustomizable);
            if (keyIsCustomizable) {
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
        apikeyPersistProperty.setValue(importerPreferences.shouldPersistCustomKeys());

        // Load custom URL templates from preferences if they exist
        Map<String, String> savedTemplates = importerPreferences.getSearchEngineUrlTemplates();
        if (!savedTemplates.isEmpty()) {
            searchEngines.clear();
            savedTemplates.forEach((name, url) -> searchEngines.add(new SearchEngineItem(name, url)));
        }
    }

    @Override
    public void storeSettings() {
        importerPreferences.setImporterEnabled(enableWebSearchProperty.getValue());
        importerPreferences.setWarnAboutDuplicatesOnImport(warnAboutDuplicatesOnImportProperty.getValue());
        filePreferences.setDownloadLinkedFiles(shouldDownloadLinkedOnlineFiles.getValue());
        filePreferences.setKeepDownloadUrl(shouldKeepDownloadUrl.getValue());
        libraryPreferences.setAddImportedEntries(addImportedEntries.getValue());
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
        importerPreferences.getApiKeys().clear();
        if (apikeyPersistAvailableProperty.get()) {
            importerPreferences.getApiKeys().addAll(apiKeysToStore);
        }

        // Save custom URL templates to preferences
        Map<String, String> templates = searchEngines.stream()
                                                     .collect(Collectors.toMap(
                                                             SearchEngineItem::getName,
                                                             SearchEngineItem::getUrlTemplate
                                                     ));
        importerPreferences.setSearchEngineUrlTemplates(templates);
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
        return shouldKeepDownloadUrl;
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

    public ObservableList<SearchEngineItem> getSearchEngines() {
        return searchEngines;
    }

    public void checkApiKey(FetcherViewModel fetcherViewModel, String apiKey, Consumer<Boolean> onFinished) {
        Callable<Boolean> tester = () -> {
            WebFetcher webFetcher = fetcherViewModel.getFetcher();

            if (!(webFetcher instanceof CustomizableKeyFetcher fetcher)) {
                return false;
            }

            String testUrlWithoutApiKey = fetcher.getTestUrl();
            if (testUrlWithoutApiKey == null) {
                return false;
            }

            if (apiKey.isEmpty()) {
                return false;
            }

            try {
                URLDownload urlDownload = new URLDownload(testUrlWithoutApiKey + apiKey);
                // The HEAD request cannot be used because its response is not 200 (maybe 404 or 596...).
                int statusCode = ((HttpURLConnection) urlDownload.getSource().openConnection()).getResponseCode();
                return (statusCode >= 200) && (statusCode < 300);
            } catch (IOException | UnirestException e) {
                return false;
            }
        };
        BackgroundTask.wrap(tester)
                      .onSuccess(onFinished)
                      .onFailure(_ -> onFinished.accept(false))
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
