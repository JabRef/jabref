package org.jabref.gui.integrity;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabaseContext;

/**
 * Controller for the .blg file settings panel.
 *
 * Binds the path text field to the ViewModel,
 * and handles browse/reset button actions.
 */
public class BibLogSettingsPane {
    @FXML
    private TextField pathField;
    private BibLogSettingsViewModel viewModel;
    private DialogService dialogService;
    private Runnable onBlgPathChanged;

    public void initialize(BibDatabaseContext context, DialogService dialogService, Runnable onBlgPathChanged) {
        this.dialogService = dialogService;
        this.onBlgPathChanged = onBlgPathChanged;
        this.viewModel = new BibLogSettingsViewModel(context.getMetaData(), context.getDatabasePath());
        pathField.textProperty().bindBidirectional(viewModel.pathProperty());
    }

    @FXML
    private void onBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select .blg file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BibTeX log files", "*.blg"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            viewModel.setBlgFilePath(selectedFile.toPath());
            if (onBlgPathChanged != null) {
                // Notify Dialog to refresh
                onBlgPathChanged.run();
            }
        }
    }

    @FXML
    private void onReset() {
        viewModel.resetBlgFilePath();
        if (onBlgPathChanged != null) {
            onBlgPathChanged.run();
        }
    }

    public BibLogSettingsViewModel getViewModel() {
        return viewModel;
    }
}
