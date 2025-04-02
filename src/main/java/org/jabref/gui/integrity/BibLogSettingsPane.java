package org.jabref.gui.integrity;

import java.nio.file.Path;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
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
        FileDialogConfiguration fileDialogConfiguration = createBlgFileDialogConfig();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(path -> {
            viewModel.setBlgFilePath(path);
            notifyPathChanged();
        });
    }

    @FXML
    private void onReset() {
        viewModel.resetBlgFilePath();
        notifyPathChanged();
    }

    public BibLogSettingsViewModel getViewModel() {
        return viewModel;
    }

    private FileDialogConfiguration createBlgFileDialogConfig() {
        FileDialogConfiguration.Builder configBuilder = new FileDialogConfiguration.Builder();
        Path initialDir = viewModel.getInitialDirectory();
        configBuilder.withInitialDirectory(initialDir);
        configBuilder.addExtensionFilter(
                new FileChooser.ExtensionFilter(Localization.lang("BibTeX log files"), "*.blg")
        );
        return configBuilder.build();
    }

    private void notifyPathChanged() {
        Optional.ofNullable(onBlgPathChanged).ifPresent(Runnable::run);
    }
}
