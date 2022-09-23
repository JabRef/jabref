package org.jabref.logic.importer;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.preferences.FetcherApiKey;

public class ImporterPreferences {

    private final BooleanProperty generateNewKeyOnImport;
    private final BooleanProperty useArXivDoiForMoreInfo;
    private final ObservableSet<FetcherApiKey> apiKeys;
    private final ObservableSet<CustomImporter> customImportList;

    public ImporterPreferences(boolean generateNewKeyOnImport,
                               boolean useArXivDoiForMoreInfo,
                               Set<CustomImporter> customImportList,
                               Set<FetcherApiKey> apiKeys) {
        this.generateNewKeyOnImport = new SimpleBooleanProperty(generateNewKeyOnImport);
        this.useArXivDoiForMoreInfo = new SimpleBooleanProperty(useArXivDoiForMoreInfo);
        this.customImportList = FXCollections.observableSet(customImportList);
        this.apiKeys = FXCollections.observableSet(apiKeys);
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

    public boolean shouldUseArXivDoiForMoreInfo() {
        return useArXivDoiForMoreInfo.get();
    }

    public BooleanProperty useArXivDoiForMoreInfoProperty() {
        return useArXivDoiForMoreInfo;
    }

    public void setUseArXivDoiForMoreInfo(boolean useArXivDoiForMoreInfo) {
        this.useArXivDoiForMoreInfo.set(useArXivDoiForMoreInfo);
    }

    public ObservableSet<FetcherApiKey> getApiKeys() {
        return apiKeys;
    }

    public ObservableSet<CustomImporter> getCustomImportList() {
        return customImportList;
    }
}
