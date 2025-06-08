package org.jabref.logic.importer;

import java.nio.file.Path;
import java.util.HashMap;
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

import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.preferences.FetcherApiKey;

public class ImporterPreferences {
    private final BooleanProperty importerEnabled;
    private final BooleanProperty generateNewKeyOnImport;
    private final BooleanProperty warnAboutDuplicatesOnImport;
    private final ObjectProperty<Path> importWorkingDirectory;
    private final ObservableSet<FetcherApiKey> apiKeys;
    private final Map<String, String> defaultApiKeys;
    private final ObservableSet<CustomImporter> customImporters;
    private final BooleanProperty persistCustomKeys;
    private final ObservableList<String> catalogs;
    private final ObjectProperty<PlainCitationParserChoice> defaultPlainCitationParser;
    private final IntegerProperty citationsRelationsStoreTTL;
    private final Map<String, String> searchEngineUrlTemplates;

    public ImporterPreferences(boolean importerEnabled,
                               boolean generateNewKeyOnImport,
                               Path importWorkingDirectory,
                               boolean warnAboutDuplicatesOnImport,
                               Set<CustomImporter> customImporters,
                               Set<FetcherApiKey> apiKeys,
                               Map<String, String> defaultApiKeys,
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
        this.customImporters = FXCollections.observableSet(customImporters);
        this.apiKeys = FXCollections.observableSet(apiKeys);
        this.defaultApiKeys = defaultApiKeys;
        this.persistCustomKeys = new SimpleBooleanProperty(persistCustomKeys);
        this.catalogs = FXCollections.observableArrayList(catalogs);
        this.defaultPlainCitationParser = new SimpleObjectProperty<>(defaultPlainCitationParser);
        this.citationsRelationsStoreTTL = new SimpleIntegerProperty(citationsRelationsStoreTTL);
        this.searchEngineUrlTemplates = new HashMap<>(searchEngineUrlTemplates);
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

    /**
     * @param name of the fetcher
     * @return either a customized API key if configured or the default key
     */
    public Optional<String> getApiKey(String name) {
        return apiKeys.stream()
                      .filter(key -> key.getName().equalsIgnoreCase(name))
                      .filter(FetcherApiKey::shouldUse)
                      .findFirst()
                      .map(FetcherApiKey::getKey)
                      .or(() -> Optional.ofNullable(defaultApiKeys.get(name)));
    }

    public void setCatalogs(List<String> catalogs) {
        this.catalogs.clear();
        this.catalogs.addAll(catalogs);
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
