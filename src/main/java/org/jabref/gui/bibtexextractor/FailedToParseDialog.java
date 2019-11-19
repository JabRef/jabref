package org.jabref.gui.bibtexextractor;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import javax.inject.Inject;

public class FailedToParseDialog extends BaseDialog<Void> {

    private final Button buttonExtract;
    private final Button buttonParse;
    @FXML
    private TextArea input;
    @FXML private ButtonType extractButtonType;
    @FXML private ButtonType parseButtonType;
    @FXML private CheckBox directAddBox;
    private BibtexExtractorViewModel viewModel;
    private EntryByPlainTextViewModel textViewModel;
    @Inject
    private StateManager stateManager;

    public FailedToParseDialog(String oldInput){
        ViewLoader.view(this)
                 .load()
                 .setAsDialogPane(this);
        this.setTitle(Localization.lang("Extraction failed"));
        input.setText(oldInput);

        buttonExtract = (Button) getDialogPane().lookupButton(extractButtonType);
        buttonParse = (Button) getDialogPane().lookupButton(parseButtonType);
        buttonParse.setOnAction(event -> textViewModel.startParsing(directAddBox.isSelected()));
        buttonExtract.setOnAction(e -> viewModel.startExtraction());
        buttonParse.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
        buttonExtract.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        this.viewModel = new BibtexExtractorViewModel(database);
        this.textViewModel = new EntryByPlainTextViewModel(database);
        input.textProperty().bindBidirectional(viewModel.inputTextProperty());
        input.textProperty().bindBidirectional(textViewModel.inputTextProperty());
    }
}
