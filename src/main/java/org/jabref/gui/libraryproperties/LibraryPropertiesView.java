package org.jabref.gui.libraryproperties;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryPropertiesView extends BaseDialog<LibraryPropertiesViewModel> {

    public TabPane tabPane;
    @FXML private ButtonType saveButton;

    @Inject private DialogService dialogService;

    private final BibDatabaseContext databaseContext;
    private LibraryPropertiesViewModel viewModel;

    public LibraryPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> savePreferencesAndCloseDialog());

        setTitle(Localization.lang("Library properties"));
    }

    @FXML
    private void initialize() {
        viewModel = new LibraryPropertiesViewModel(databaseContext);

        for (PropertiesTab pane : viewModel.getPropertiesTabs()) {
            tabPane.getTabs().add(new Tab(pane.getTabName(), pane.getBuilder()));
            ((AbstractPropertiesTabView<?>) pane).prefWidthProperty().bind(tabPane.widthProperty());
            ((AbstractPropertiesTabView<?>) pane).getStyleClass().add("propertiesTab");
        }

        viewModel.setValues();
    }

    private void savePreferencesAndCloseDialog() {
        viewModel.storeAllSettings();
        close();
    }
}
