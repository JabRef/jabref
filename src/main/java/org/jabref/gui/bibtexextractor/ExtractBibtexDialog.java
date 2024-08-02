package org.jabref.gui.bibtexextractor;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 * Handles both online and offline case.
 *
 * @implNote Instead of using inheritance, we do if/else checks.
 *
 */
public class ExtractBibtexDialog extends BaseDialog<Void> {

    @Inject protected StateManager stateManager;
    @Inject protected DialogService dialogService;
    @Inject protected FileUpdateMonitor fileUpdateMonitor;
    @Inject protected TaskExecutor taskExecutor;
    @Inject protected UndoManager undoManager;
    @Inject protected PreferencesService preferencesService;

    @FXML protected TextArea input;
    private final boolean onlineMode;

    @FXML private ButtonType parseButtonType;

    public ExtractBibtexDialog(boolean onlineMode) {
        this.onlineMode = onlineMode;
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        if (onlineMode) {
            this.setTitle(Localization.lang("Plain References Parser (online)"));
        } else {
            this.setTitle(Localization.lang("Plain References Parser (offline)"));
        }
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        BibtexExtractorViewModel viewModel = new BibtexExtractorViewModel(
                onlineMode,
                database,
                dialogService,
                preferencesService,
                fileUpdateMonitor,
                taskExecutor,
                undoManager,
                stateManager);

        input.textProperty().bindBidirectional(viewModel.inputTextProperty());
        String clipText = ClipBoardManager.getContents();
        if (StringUtil.isBlank(clipText)) {
            input.setPromptText(Localization.lang("Please enter the plain references to extract from separated by double empty lines."));
        } else {
            input.setText(clipText);
            input.selectAll();
        }

        Platform.runLater(() -> {
            input.requestFocus();
            Button buttonParse = (Button) getDialogPane().lookupButton(parseButtonType);
            buttonParse.setTooltip(new Tooltip((Localization.lang("Starts the extraction and adds the resulting entries to the currently opened database"))));
            buttonParse.setOnAction(event -> viewModel.startParsing());
            buttonParse.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
        });
    }
}
