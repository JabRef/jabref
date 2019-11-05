package org.jabref.gui.bibtexextractor;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import org.jabref.gui.StateManager;
import org.jabref.gui.entrybyplaintext.EntryByPlainTextViewModel;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import javax.inject.Inject;

public class FailedToExtractDialog extends BaseDialog<Void> {

    private final Button buttonExtract;
    private final Button buttonParse;
    @FXML
    private TextArea input;
    @FXML private ButtonType extractButtonType;
    @FXML private ButtonType parseButtonType;
    private BibtexExtractorViewModel viewModel;
    private EntryByPlainTextViewModel textViewModel;
    @Inject
    private StateManager stateManager;

    public FailedToExtractDialog(String oldInput){

        ViewLoader.view(this)
                 .load()
                  .setAsDialogPane(this);

        this.setTitle(Localization.lang("Extraction failed"));

        buttonExtract = (Button) getDialogPane().lookupButton(extractButtonType);
        buttonParse = (Button) getDialogPane().lookupButton(parseButtonType);
        buttonParse.setTooltip(new Tooltip());
        buttonParse.setOnAction(event -> textViewModel.startParsing());
        buttonExtract.setTooltip(new Tooltip((Localization.lang("Starts the extraction of the BibTeX entry"))));
        buttonExtract.setOnAction(e -> viewModel.startExtraction());
        buttonParse.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
        buttonExtract.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
        input.setText(oldInput);
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        this.viewModel = new BibtexExtractorViewModel(database);
        this.textViewModel = new EntryByPlainTextViewModel(database);
        input.textProperty().bindBidirectional(viewModel.inputTextProperty());
    }
}
