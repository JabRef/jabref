package org.jabref.gui.bibtexextractor;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 */
public class ExtractBibtexDialog extends BaseDialog<Void> {

    private final Button buttonParse;
    private final Button buttonToNewLib;
    @FXML private TextArea input;
    @FXML private ButtonType parseButtonType;
    @FXML private ButtonType parseToNewLibraryType;
    private BibtexExtractorViewModel viewModel;
    private boolean directAdd;
    @Inject private StateManager stateManager;

    public ExtractBibtexDialog() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
        this.setTitle(Localization.lang("JabRef Parser"));
        input.setPromptText(Localization.lang("Please enter the text to extract from."));
        input.selectAll();

        buttonParse = (Button) getDialogPane().lookupButton(parseButtonType);
        buttonToNewLib = (Button) getDialogPane().lookupButton(parseToNewLibraryType);
        buttonParse.setOnAction(event -> {
            directAdd = false;
            viewModel.startParsing(directAdd);});
        buttonToNewLib.setOnAction(event ->{
          directAdd = true;
          viewModel.startParsing(directAdd);});
        buttonParse.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
        buttonToNewLib.disableProperty().bind(viewModel.inputTextProperty().isEmpty());

    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        this.viewModel = new BibtexExtractorViewModel(database);
        input.textProperty().bindBidirectional(viewModel.inputTextProperty());
    }
}
