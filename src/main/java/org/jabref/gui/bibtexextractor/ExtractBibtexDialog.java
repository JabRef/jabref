package org.jabref.gui.bibtexextractor;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import jakarta.inject.Inject;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 */
public abstract class ExtractBibtexDialog extends BaseDialog<Void> {

    @Inject protected StateManager stateManager;
    @Inject protected DialogService dialogService;
    @Inject protected FileUpdateMonitor fileUpdateMonitor;
    @Inject protected TaskExecutor taskExecutor;
    @Inject protected UndoManager undoManager;
    @Inject protected PreferencesService preferencesService;

    @FXML protected TextArea input;

    @FXML private ButtonType parseButtonType;

    protected abstract BibtexExtractorViewModel getViewModel(BibDatabaseContext database);

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        BibtexExtractorViewModel viewModel = getViewModel(database);

        input.textProperty().bindBidirectional(viewModel.inputTextProperty());
        input.setPromptText(Localization.lang("Please enter the plain references to extract from separated by double empty lines."));
        input.selectAll();

        Platform.runLater(() -> {
            input.requestFocus();
            Button buttonParse = (Button) getDialogPane().lookupButton(parseButtonType);
            buttonParse.setTooltip(new Tooltip((Localization.lang("Starts the extraction and adds the resulting entries to the currently opened database"))));
            buttonParse.setOnAction(event -> viewModel.startParsing());
            buttonParse.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
        });
    }
}
