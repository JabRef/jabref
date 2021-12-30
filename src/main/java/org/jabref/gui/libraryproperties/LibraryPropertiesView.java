package org.jabref.gui.libraryproperties;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

public class LibraryPropertiesView extends BaseDialog<LibraryPropertiesViewModel> {

    @FXML private TabPane tabPane;
    @FXML private ButtonType saveButton;

    private final BibDatabaseContext databaseContext;
    private LibraryPropertiesViewModel viewModel;

    public LibraryPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> savePreferencesAndCloseDialog());

        if (databaseContext.getDatabasePath().isPresent()) {
            setTitle(Localization.lang("%0 - Library properties", databaseContext.getDatabasePath().get().getFileName()));
        } else {
            setTitle(Localization.lang("Library properties"));
        }
    }

    @FXML
    private void initialize() {
        viewModel = new LibraryPropertiesViewModel(databaseContext);

        for (PropertiesTab pane : viewModel.getPropertiesTabs()) {
            ScrollPane scrollPane = new ScrollPane(pane.getBuilder());
            scrollPane.setFitToHeight(true);
            scrollPane.setFitToWidth(true);
            tabPane.getTabs().add(new Tab(pane.getTabName(), scrollPane));
            if (pane instanceof AbstractPropertiesTabView<?> propertiesTab) {
                propertiesTab.prefHeightProperty().bind(tabPane.tabMaxHeightProperty());
                propertiesTab.prefWidthProperty().bind(tabPane.widthProperty());
                propertiesTab.getStyleClass().add("propertiesTab");
            }
        }

        viewModel.setValues();
    }

    private void savePreferencesAndCloseDialog() {
        viewModel.storeAllSettings();
        close();
    }
}
