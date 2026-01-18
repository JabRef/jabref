package org.jabref.gui.libraryproperties.git;

import java.util.Collections;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

/**
 * View model for the "Git" tab in the Library Properties dialog.
 * <p>
 * This class manages the state of the Git integration settings. It reads the configuration
 * from the database metadata and writes changes back to it when the user applies the settings.
 */
public class GitPropertiesViewModel implements PropertiesTabViewModel {
    public static final String LEGACY_GIT_ENABLED = "gitEnabled";

    public static final String GIT_AUTO_PULL = "gitAutoPull";
    public static final String GIT_AUTO_COMMIT = "gitAutoCommit";
    public static final String GIT_AUTO_PUSH = "gitAutoPush";

    private final BibDatabaseContext databaseContext;
    private final BooleanProperty autoPullProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoCommitProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoPushProperty = new SimpleBooleanProperty();

    private final MetaData metaData;

    public GitPropertiesViewModel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.metaData = databaseContext.getMetaData();
    }

    public BooleanProperty autoPullProperty() {
        return autoPullProperty;
    }

    public BooleanProperty autoCommitProperty() {
        return autoCommitProperty;
    }

    public BooleanProperty autoPushProperty() {
        return autoPushProperty;
    }

    @Override
    public void setValues() {
        boolean isGitEnabled = metaData.getUnknownMetaData().containsKey(GitPropertiesViewModel.LEGACY_GIT_ENABLED)
                && metaData.getUnknownMetaData().get(GitPropertiesViewModel.LEGACY_GIT_ENABLED).contains("true");

        autoPullProperty.set(isMetaDataSet(GIT_AUTO_PULL) || isGitEnabled);
        autoCommitProperty.set(isMetaDataSet(GIT_AUTO_COMMIT) || isGitEnabled);
        autoPushProperty.set(isMetaDataSet(GIT_AUTO_PUSH) || isGitEnabled);
    }

    private boolean isMetaDataSet(String key) {
        return metaData.getUnknownMetaData().containsKey(key)
                && metaData.getUnknownMetaData().get(key).contains("true");
    }

    @Override
    public void storeSettings() {
        updateMetaData(GIT_AUTO_PULL, autoPullProperty.get());
        updateMetaData(GIT_AUTO_COMMIT, autoCommitProperty.get());
        updateMetaData(GIT_AUTO_PUSH, autoPushProperty.get());

        if (metaData.getUnknownMetaData().containsKey(LEGACY_GIT_ENABLED)) {
            metaData.removeUnknownMetaDataItem(LEGACY_GIT_ENABLED);
        }
    }

    private void updateMetaData(String key, boolean value) {
        if (value) {
            metaData.putUnknownMetaDataItem(key, Collections.singletonList("true"));
        } else {
            metaData.removeUnknownMetaDataItem(key);
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }
}
