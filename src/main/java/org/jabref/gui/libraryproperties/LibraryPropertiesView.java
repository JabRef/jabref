package org.jabref.gui.libraryproperties;

import javafx.fxml.FXML;

import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jabref.gui.DialogService;
import org.jabref.gui.libraryproperties.general.GeneralPropertiesView;
import org.jabref.gui.libraryproperties.general.GeneralPropertiesViewModel;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class LibraryPropertiesView extends BaseDialog<LibraryPropertiesViewModel> {

    @FXML private TabPane tabPane;
    @FXML private ButtonType saveButton;

    @Inject private ThemeManager themeManager;
    @Inject private DialogService dialogService; // Injected DialogService

    private final BibDatabaseContext databaseContext;
    private LibraryPropertiesViewModel viewModel;

    public LibraryPropertiesView(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;

        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> savePreferencesAndCloseDialog());

        setTitle(databaseContext.getDatabasePath()
                .map(path -> Localization.lang("%0 - Library properties", path.getFileName()))
                .orElse(Localization.lang("Library properties")));

        themeManager.updateFontStyle(getDialogPane().getScene());
    }

    @FXML
    private void initialize() {
        viewModel = new LibraryPropertiesViewModel(databaseContext);

        for (PropertiesTab pane : viewModel.getPropertiesTabs()) {
            ScrollPane scrollPane = new ScrollPane(pane.getBuilder());
            scrollPane.setFitToHeight(true);
            scrollPane.setFitToWidth(true);
            Tab tab = new Tab(pane.getTabName(), scrollPane);
            tabPane.getTabs().add(tab);

            if (pane instanceof AbstractPropertiesTabView<?> propertiesTab) {
                propertiesTab.prefHeightProperty().bind(tabPane.tabMaxHeightProperty());
                propertiesTab.prefWidthProperty().bind(tabPane.widthProperty());
                propertiesTab.getStyleClass().add("propertiesTab");
            }
        }

        viewModel.setValues();
    }

    @FXML
    private void savePreferencesAndCloseDialog() {
        // Step 1: Retrieve the GeneralPropertiesViewModel
        GeneralPropertiesView generalView = (GeneralPropertiesView) viewModel.getPropertiesTabs().stream()
                .filter(tab -> tab instanceof GeneralPropertiesView)
                .findFirst()
                .orElse(null);
        if (generalView == null) {
            dialogService.showErrorDialogAndWait(
                    "Error",
                    "General properties tab is missing. Unable to save settings."
            );
            return;
        }

        GeneralPropertiesViewModel generalViewModel = generalView.getViewModel();

        // Step 2: Validate paths
        if (!generalViewModel.validatePaths()) {
            dialogService.showErrorDialogAndWait(
                    "Invalid Paths",
                    "One or more paths are invalid. Please correct them before saving."
            );
            return;
        }

        // Step 3: Save settings if validation is successful
        viewModel.storeAllSettings();
        close();
    }
}
