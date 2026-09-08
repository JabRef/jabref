package org.jabref.gui.libraryproperties;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import jakarta.inject.Inject;

public abstract class AbstractPropertiesTabView<T extends PropertiesTabViewModel> extends VBox implements PropertiesTab {

    @Inject protected DialogService dialogService;

    protected BibDatabaseContext databaseContext;
    protected T viewModel;

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues(MetaData metaData) {
        viewModel.setValues(metaData);
    }

    @Override
    public void storeSettings(MetaData metaData) {
        viewModel.storeSettings(metaData);
    }

    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }
}
