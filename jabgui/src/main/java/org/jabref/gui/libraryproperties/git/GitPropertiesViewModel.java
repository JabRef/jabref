package org.jabref.gui.libraryproperties.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

/// View model for the "Git" tab in the Library Properties dialog.
///
/// This class manages the state of the Git integration settings. It reads the configuration
/// from the database metadata and writes changes back to it when the user applies the settings.
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
        autoPullProperty.set(metaData.isGitAutoPullEnabled());
        autoCommitProperty.set(metaData.isGitAutoCommitEnabled());
        autoPushProperty.set(metaData.isGitAutoPushEnabled());
    }

    @Override
    public void storeSettings() {
        metaData.setGitAutoPullEnabled(autoPullProperty.get());
        metaData.setGitAutoCommitEnabled(autoCommitProperty.get());
        metaData.setGitAutoPushEnabled(autoPushProperty.get());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }
}
