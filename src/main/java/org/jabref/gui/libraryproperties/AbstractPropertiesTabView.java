package org.jabref.gui.libraryproperties;

import javax.inject.Inject;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabaseContext;

public abstract class AbstractPropertiesTabView<T extends PropertiesTabViewModel> extends VBox implements PropertiesTab {

    @Inject protected DialogService dialogService;

    protected BibDatabaseContext databaseContext;
    protected T viewModel;

    @Override
    public Node getBuilder() {
        return this;
    }

    @Override
    public void setValues() {
        viewModel.setValues();
    }

    @Override
    public void storeSettings() {
        viewModel.storeSettings();
    }

    @Override
    public boolean validateSettings() {
        return viewModel.validateSettings();
    }
}
