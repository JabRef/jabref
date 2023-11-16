package org.jabref.logic.importer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.preferences.FetcherApiKey;

public class ImporterPreferences {

    private final BooleanProperty importerEnabled;
    private final BooleanProperty generateNewKeyOnImport;
    private final BooleanProperty warnAboutDuplicatesOnImport;
    private final ObjectProperty<Path> importWorkingDirectory;
    private final ObservableSet<FetcherApiKey> apiKeys;
    private final ObservableSet<CustomImporter> customImporters;
    private final BooleanProperty persistCustomKeys;
    private final ObservableList<String> catalogs;
    public ImporterPreferences(boolean importerEnabled,
                               boolean generateNewKeyOnImport,
                               Path importWorkingDirectory,
                               boolean warnAboutDuplicatesOnImport,
                               Set<CustomImporter> customImporters,
                               Set<FetcherApiKey> apiKeys,
                               boolean persistCustomKeys,
                               List<String> catalogs) {
        this.importerEnabled = new SimpleBooleanProperty(importerEnabled);
        this.generateNewKeyOnImport = new SimpleBooleanProperty(generateNewKeyOnImport);
        this.importWorkingDirectory = new SimpleObjectProperty<>(importWorkingDirectory);
        this.warnAboutDuplicatesOnImport = new SimpleBooleanProperty(warnAboutDuplicatesOnImport);
        this.customImporters = FXCollections.observableSet(customImporters);
        this.apiKeys = FXCollections.observableSet(apiKeys);
        this.persistCustomKeys = new SimpleBooleanProperty(persistCustomKeys);
        this.catalogs = FXCollections.observableArrayList(catalogs);
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

    public boolean isGenerateNewKeyOnImport() {
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

    public Optional<String> getApiKey(String name) {
        return apiKeys.stream()
                      .filter(key -> key.getName().equalsIgnoreCase(name))
                      .filter(FetcherApiKey::shouldUse)
                      .findFirst()
                      .map(FetcherApiKey::getKey);
    }

    public void setCatalogs(List<String> catalogs) {
        this.catalogs.clear();
        this.catalogs.addAll(catalogs);
    }

    public ObservableList<String> getCatalogs() {
          return catalogs;
    }
}
