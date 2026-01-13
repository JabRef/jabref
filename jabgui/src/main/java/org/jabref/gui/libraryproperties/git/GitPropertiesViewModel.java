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
    private final BibDatabaseContext databaseContext;
    private final BooleanProperty autoCommitProperty = new SimpleBooleanProperty();
    private final MetaData metaData;

    public GitPropertiesViewModel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        this.metaData = databaseContext.getMetaData();
    }

    public BooleanProperty autoCommitProperty() {
        return autoCommitProperty;
    }

    @Override
    public void setValues() {
        boolean isGitEnabled = metaData.getUnknownMetaData().containsKey("gitEnabled")
                && metaData.getUnknownMetaData().get("gitEnabled").contains("true");

        autoCommitProperty.set(isGitEnabled);
    }

    @Override
    public void storeSettings() {
        if (autoCommitProperty.get()) {
            metaData.putUnknownMetaDataItem("gitEnabled", Collections.singletonList("true"));
        } else {
            metaData.removeUnknownMetaDataItem("gitEnabled");
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }
}
