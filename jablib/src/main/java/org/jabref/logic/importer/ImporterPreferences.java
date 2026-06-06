package org.jabref.logic.importer;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.logic.importer.fetcher.ACMPortalFetcher;
import org.jabref.logic.importer.fetcher.AstrophysicsDataSystem;
import org.jabref.logic.importer.fetcher.BiodiversityLibrary;
import org.jabref.logic.importer.fetcher.DBLPFetcher;
import org.jabref.logic.importer.fetcher.IEEE;
import org.jabref.logic.importer.fetcher.Scopus;
import org.jabref.logic.importer.fetcher.SpringerNatureWebFetcher;
import org.jabref.logic.importer.fetcher.WileyFetcher;
import org.jabref.logic.importer.fetcher.citation.semanticscholar.SemanticScholarCitationFetcher;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.preferences.FetcherApiKey;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.Directories;

public class ImporterPreferences {
    private final BooleanProperty importerEnabled;
    private final BooleanProperty generateNewKeyOnImport;
    private final BooleanProperty warnAboutDuplicatesOnImport;
    private final ObjectProperty<Path> importWorkingDirectory;
    private final ObservableSet<FetcherApiKey> apiKeys;
    private final ObservableSet<CustomImporter> customImporters;
    private final BooleanProperty persistCustomKeys;
    private final ObservableList<String> catalogs;
    private final ObjectProperty<PlainCitationParserChoice> defaultPlainCitationParser;
    private final IntegerProperty citationsRelationsStoreTTL;
    private final Map<String, String> searchEngineUrlTemplates;

    private ImporterPreferences() {
        this(
                true,                                          // Importers enabled
                true,                                          // Generate new key on import
                Directories.getUserDirectory(),                // Import working directory
                true,                                          // Warn about duplicates on import
                Set.of(),                                      // Custom importers
                Set.of(),                                      // API keys
                false,                                         // Persist custom keys
                List.of(ACMPortalFetcher.FETCHER_NAME,         // Catalogs
                        SpringerNatureWebFetcher.FETCHER_NAME,
                        DBLPFetcher.FETCHER_NAME,
                        IEEE.FETCHER_NAME),
                PlainCitationParserChoice.RULE_BASED_GENERAL,  // Default plain citation parser
                30,                                            // Citations relations store TTL
                Map.of()                                       // Search engine URL templates
        );
    }

    public ImporterPreferences(boolean importerEnabled,
                               boolean generateNewKeyOnImport,
                               Path importWorkingDirectory,
                               boolean warnAboutDuplicatesOnImport,
                               Set<CustomImporter> customImporters,
                               Set<FetcherApiKey> apiKeys,
                               boolean persistCustomKeys,
                               List<String> catalogs,
                               PlainCitationParserChoice defaultPlainCitationParser,
                               int citationsRelationsStoreTTL,
                               Map<String, String> searchEngineUrlTemplates
    ) {
        this.importerEnabled = new SimpleBooleanProperty(importerEnabled);
        this.generateNewKeyOnImport = new SimpleBooleanProperty(generateNewKeyOnImport);
        this.importWorkingDirectory = new SimpleObjectProperty<>(importWorkingDirectory);
        this.warnAboutDuplicatesOnImport = new SimpleBooleanProperty(warnAboutDuplicatesOnImport);
        this.customImporters = FXCollections.observableSet(new HashSet<>(customImporters));
        this.apiKeys = FXCollections.observableSet(new HashSet<>(apiKeys));
        this.persistCustomKeys = new SimpleBooleanProperty(persistCustomKeys);
        this.catalogs = FXCollections.observableArrayList(catalogs);
        this.defaultPlainCitationParser = new SimpleObjectProperty<>(defaultPlainCitationParser);
        this.citationsRelationsStoreTTL = new SimpleIntegerProperty(citationsRelationsStoreTTL);
        this.searchEngineUrlTemplates = new HashMap<>(searchEngineUrlTemplates);
    }

    public static ImporterPreferences getDefault() {
        ImporterPreferences preferences = new ImporterPreferences();
        preferences.setApiKeys(new HashSet<>(getDefaultFetcherKeys()
                .entrySet().stream()
                .map(entry -> new FetcherApiKey(entry.getKey(), false, entry.getValue()))
                .toList()));
        return preferences;
    }

    private static Map<String, String> getDefaultFetcherKeys() {
        // To avoid including a heavy dependency tree with the current dependency injector of afterburner.fx, we
        // instantiate "BuildInfo" directly.
        BuildInfo buildInfo = new BuildInfo();

        return Map.of(
                AstrophysicsDataSystem.FETCHER_NAME, buildInfo.astrophysicsDataSystemAPIKey,
                BiodiversityLibrary.FETCHER_NAME, buildInfo.biodiversityHeritageApiKey,
                Scopus.FETCHER_NAME, buildInfo.scopusApiKey,
                SemanticScholarCitationFetcher.FETCHER_NAME, buildInfo.semanticScholarApiKey,
                // SpringerLink uses the same key and fetcher name as SpringerFetcher
                SpringerNatureWebFetcher.FETCHER_NAME, buildInfo.springerNatureAPIKey,
                WileyFetcher.FETCHER_NAME, buildInfo.wileyTdmApiKey
        );
    }

    public void setAll(ImporterPreferences preferences) {
        this.importerEnabled.set(preferences.areImporterEnabled());
        this.generateNewKeyOnImport.set(preferences.shouldGenerateNewKeyOnImport());
        this.importWorkingDirectory.set(preferences.getImportWorkingDirectory());
        this.warnAboutDuplicatesOnImport.set(preferences.shouldWarnAboutDuplicatesOnImport());
        setCustomImporters(preferences.getCustomImporters());
        this.persistCustomKeys.set(preferences.shouldPersistCustomKeys()); // Before getApiKeys to avoid stale keys in keyring
        setApiKeys(preferences.getApiKeys());
        this.catalogs.setAll(preferences.getCatalogs());
        this.defaultPlainCitationParser.set(preferences.getDefaultPlainCitationParser());
        this.citationsRelationsStoreTTL.set(preferences.getCitationsRelationsStoreTTL());
        setSearchEngineUrlTemplates(preferences.getSearchEngineUrlTemplates());
    }

    public boolean areImporterEnabled() {
        return importerEnabled.get();
    }

    public BooleanProperty importerEnabledProperty() {
        return importerEnabled;
    }

    public void setImporterEnabled(boolean importerEnabled) {
        this.importerEnabled.set(importerEnabled);
    }

    public boolean shouldGenerateNewKeyOnImport() {
        return generateNewKeyOnImport.get();
    }

    public BooleanProperty generateNewKeyOnImportProperty() {
        return generateNewKeyOnImport;
    }

    public void setGenerateNewKeyOnImport(boolean generateNewKeyOnImport) {
        this.generateNewKeyOnImport.set(generateNewKeyOnImport);
    }

    public Path getImportWorkingDirectory() {
        return importWorkingDirectory.get();
    }

    public ObjectProperty<Path> importWorkingDirectoryProperty() {
        return importWorkingDirectory;
    }

    public void setImportWorkingDirectory(Path importWorkingDirectory) {
        this.importWorkingDirectory.set(importWorkingDirectory);
    }

    public boolean shouldWarnAboutDuplicatesOnImport() {
        return warnAboutDuplicatesOnImport.get();
    }

    public BooleanProperty warnAboutDuplicatesOnImportProperty() {
        return warnAboutDuplicatesOnImport;
    }

    public void setWarnAboutDuplicatesOnImport(boolean warnAboutDuplicatesOnImport) {
        this.warnAboutDuplicatesOnImport.set(warnAboutDuplicatesOnImport);
    }

    public void setApiKeys(Set<FetcherApiKey> apiKeys) {
        this.apiKeys.clear();
        this.apiKeys.addAll(apiKeys);
    }

    public ObservableSet<FetcherApiKey> getApiKeys() {
        return apiKeys;
    }

    public ObservableSet<CustomImporter> getCustomImporters() {
        return customImporters;
    }

    public void setCustomImporters(Set<CustomImporter> importers) {
        customImporters.clear();
        customImporters.addAll(importers);
    }

    public boolean shouldPersistCustomKeys() {
        return persistCustomKeys.get();
    }

    public BooleanProperty persistCustomKeysProperty() {
        return persistCustomKeys;
    }

    public void setPersistCustomKeys(boolean persistCustomKeys) {
        this.persistCustomKeys.set(persistCustomKeys);
    }

    /// @param name of the fetcher
    /// @return either a customized API key if configured or the default key
    /// @implNote See `fetchers.md` for general information on fetchers.
    public Optional<String> getApiKey(String name) {
        return apiKeys.stream()
                      .filter(key -> key.getName().equalsIgnoreCase(name))
                      .filter(FetcherApiKey::shouldUse)
                      .findFirst()
                      .map(FetcherApiKey::getKey)
                      .or(() -> Optional.ofNullable(getDefaultFetcherKeys().get(name)));
    }

    public void setCatalogs(List<String> catalogs) {
        this.catalogs.setAll(catalogs);
    }

    public ObservableList<String> getCatalogs() {
        return catalogs;
    }

    public PlainCitationParserChoice getDefaultPlainCitationParser() {
        return defaultPlainCitationParser.get();
    }

    public ObjectProperty<PlainCitationParserChoice> defaultPlainCitationParserProperty() {
        return defaultPlainCitationParser;
    }

    public void setDefaultPlainCitationParser(PlainCitationParserChoice defaultPlainCitationParser) {
        this.defaultPlainCitationParser.set(defaultPlainCitationParser);
    }

    public int getCitationsRelationsStoreTTL() {
        return this.citationsRelationsStoreTTL.get();
    }

    public IntegerProperty citationsRelationsStoreTTLProperty() {
        return this.citationsRelationsStoreTTL;
    }

    public void setCitationsRelationsStoreTTL(int citationsRelationsStoreTTL) {
        this.citationsRelationsStoreTTL.set(citationsRelationsStoreTTL);
    }

    public Map<String, String> getSearchEngineUrlTemplates() {
        return searchEngineUrlTemplates;
    }

    public void setSearchEngineUrlTemplates(Map<String, String> templates) {
        searchEngineUrlTemplates.clear();
        searchEngineUrlTemplates.putAll(templates);
    }
}
