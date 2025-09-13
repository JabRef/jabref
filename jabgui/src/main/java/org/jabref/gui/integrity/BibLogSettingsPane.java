package org.jabref.gui.integrity;

import java.nio.file.Path;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.JabRefException;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the .blg file settings panel.
 * <p>
 * Binds the path text field to the ViewModel,
 * and handles browse/reset button actions.
 */
public class BibLogSettingsPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibLogSettingsPane.class);
    @FXML
    private TextField pathField;
    private BibLogSettingsViewModel viewModel;
    @Inject private DialogService dialogService;
    private Runnable onBlgPathChanged;

    public void initializeViewModel(BibDatabaseContext context, Runnable onBlgPathChanged) throws JabRefException {
        this.onBlgPathChanged = onBlgPathChanged;
        this.viewModel = new BibLogSettingsViewModel(context.getMetaData(), context.getDatabasePath());
        pathField.textProperty().bindBidirectional(viewModel.pathProperty());
        viewModel.getBlgWarnings(context);
    }

    public ObservableList<IntegrityMessage> getBlgWarnings() {
        return viewModel.getBlgWarningsObservable();
    }

    public void refreshWarnings(BibDatabaseContext context) throws JabRefException {
        viewModel.getBlgWarnings(context);
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

    private void notifyPathChanged() {
        if (onBlgPathChanged != null) {
            onBlgPathChanged.run();
        }
    }

    public BibLogSettingsViewModel getViewModel() {
        return viewModel;
    }

    private FileDialogConfiguration createBlgFileDialogConfig() {
        Path initialDir = viewModel.getInitialDirectory();
        FileDialogConfiguration config = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("BibTeX log files"), StandardFileType.BLG)
                .withDefaultExtension(Localization.lang("BibTeX log files"), StandardFileType.BLG)
                .withInitialDirectory(viewModel.getInitialDirectory())
                .build();
        return config;
    }

    public boolean wasBlgFileManuallySelected() {
        return viewModel.wasBlgFileManuallySelected();
    }
}
