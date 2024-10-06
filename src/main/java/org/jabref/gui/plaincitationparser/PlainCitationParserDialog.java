package org.jabref.gui.plaincitationparser;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 * Handles both online and offline case.
 *
 * @implNote Instead of using inheritance, we do if/else checks.
 *
 */
public class PlainCitationParserDialog extends BaseDialog<Void> {
    @Inject protected StateManager stateManager;
    @Inject protected DialogService dialogService;
    @Inject protected AiService aiService;
    @Inject protected FileUpdateMonitor fileUpdateMonitor;
    @Inject protected TaskExecutor taskExecutor;
    @Inject protected UndoManager undoManager;
    @Inject protected GuiPreferences preferences;

    @FXML protected TextArea input;
    @FXML protected ButtonType parseButtonType;
    @FXML protected ComboBox<PlainCitationParserChoice> parserChoice;

    public PlainCitationParserDialog() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.setTitle(Localization.lang("Plain Citations Parser"));
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));

        PlainCitationParserViewModel viewModel = new PlainCitationParserViewModel(
                database,
                dialogService,
                aiService,
                preferences,
                fileUpdateMonitor,
                taskExecutor,
                undoManager,
                stateManager);

        new ViewModelListCellFactory<PlainCitationParserChoice>()
                .withText(PlainCitationParserChoice::getLocalizedName)
                .install(parserChoice);
        parserChoice.getItems().setAll(viewModel.plainCitationParsers());
        parserChoice.valueProperty().bindBidirectional(viewModel.parserChoice());

        input.textProperty().bindBidirectional(viewModel.inputTextProperty());

        String clipText = ClipBoardManager.getContents();
        if (StringUtil.isBlank(clipText)) {
            input.setPromptText(Localization.lang("Please enter the plain citations to parse from separated by double empty lines."));
        } else {
            input.setText(clipText);
            input.selectAll();
        }

        Platform.runLater(() -> {
            input.requestFocus();
            Button buttonParse = (Button) getDialogPane().lookupButton(parseButtonType);
            buttonParse.setTooltip(new Tooltip((Localization.lang("Starts the parsing and adds the resulting entries to the currently opened database"))));
            buttonParse.setOnAction(event -> viewModel.startParsing());
            buttonParse.disableProperty().bind(viewModel.inputTextProperty().isEmpty());
        });
    }
}
