package org.jabref.gui.libraryproperties.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.libraryproperties.PropertiesTabViewModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class GitPropertiesViewModel implements PropertiesTabViewModel {
    private final BooleanProperty autoCommitProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoPushProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoPullProperty = new SimpleBooleanProperty();

    private final BibDatabaseContext databaseContext;

    GitPropertiesViewModel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    @Override
    public void setValues() {
        MetaData metaData = databaseContext.getMetaData();
        autoCommitProperty.setValue(metaData.isGitAutoCommit());
        autoPushProperty.setValue(metaData.isGitAutoPush());
        autoPullProperty.setValue(metaData.isGitAutoPull());
    }

    @Override
    public void storeSettings() {
        MetaData metaData = databaseContext.getMetaData();
        metaData.setGitAutoCommit(autoCommitProperty.getValue());
        metaData.setGitAutoPush(autoPushProperty.getValue());
        metaData.setGitAutoPull(autoPullProperty.getValue());
    }

    public BooleanProperty autoCommitProperty() {
        return autoCommitProperty;
    }

    public BooleanProperty autoPushProperty() {
        return autoPushProperty;
    }

    public BooleanProperty autoPullProperty() {
        return autoPullProperty;
    }
}
