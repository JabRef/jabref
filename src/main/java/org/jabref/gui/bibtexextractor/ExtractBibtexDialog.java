package org.jabref.gui.bibtexextractor;

import javax.inject.Inject;
import javax.swing.undo.UndoManager;

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

import com.airhacks.afterburner.views.ViewLoader;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 */
public class ExtractBibtexDialog extends BaseDialog<Void> {

    private final Button buttonParse;
    @FXML private TextArea input;
    @FXML private ButtonType parseButtonType;
    private BibtexExtractorViewModel viewModel;
    @Inject private StateManager stateManager;
    @Inject private DialogService dialogService;
    @Inject private FileUpdateMonitor fileUpdateMonitor;
    @Inject private TaskExecutor taskExecutor;
    @Inject private UndoManager undoManager;
    @Inject private PreferencesService preferencesService;

    public ExtractBibtexDialog() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        this.setTitle(Localization.lang("Plain References Parser"));
        input.setPromptText(Localization.lang("Please enter the plain references to extract from separated by double empty lines."));
        input.selectAll();

        buttonParse = (Button) getDialogPane().lookupButton(parseButtonType);
        buttonParse.setTooltip(new Tooltip((Localization.lang("Starts the extraction and adds the resulting entries to the currently opened database"))));
        buttonParse.setOnAction((event) -> viewModel.startParsing());
        buttonParse.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        this.viewModel = new BibtexExtractorViewModel(database, dialogService, preferencesService, fileUpdateMonitor, taskExecutor, undoManager, stateManager);
        input.textProperty().bindBidirectional(viewModel.inputTextProperty());
    }
}
