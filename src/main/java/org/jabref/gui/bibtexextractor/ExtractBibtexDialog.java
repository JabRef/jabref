package org.jabref.gui.bibtexextractor;

import javax.inject.Inject;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 */
public class ExtractBibtexDialog extends BaseDialog<Void> {

    private final Button buttonExtract;
    @FXML private TextArea input;
    @FXML private ButtonType extractButtonType;
    private BibtexExtractorViewModel viewModel;

    @Inject private StateManager stateManager;

    public ExtractBibtexDialog() {

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setTitle(Localization.lang("Input text to parse"));
        buttonExtract = (Button) getDialogPane().lookupButton(extractButtonType);
        buttonExtract.setTooltip(new Tooltip((Localization.lang("Starts the extraction of the BibTeX entry"))));
        buttonExtract.setOnAction(e -> viewModel.startExtraction());
        buttonExtract.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        this.viewModel = new BibtexExtractorViewModel(database);

        input.textProperty().bindBidirectional(viewModel.inputTextProperty());
    }
}
