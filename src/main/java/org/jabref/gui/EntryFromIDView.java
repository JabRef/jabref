package org.jabref.gui;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import org.fxmisc.easybind.EasyBind;
import org.jabref.Globals;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.preferences.JabRefPreferences;

/**
 * Dialog that prompts the user to enter an id for an entry.
 * Returns null if canceled.
 */
public class EntryFromIDView extends BaseDialog<EntryType> {

    @FXML private ButtonType generateButton;
    @FXML private ButtonType lookupButton;
    @FXML private TextField idTextField;
    @FXML private Label lookUpField;

    private final BasePanel basePanel;
    private final DialogService dialogService;
    private final JabRefPreferences prefs;

    private EntryType type;
    private EntryFromIDViewModel viewModel;

    public EntryFromIDView(BasePanel basePanel, DialogService dialogService, JabRefPreferences preferences) {
        this.basePanel = basePanel;
        this.dialogService = dialogService;
        this.prefs = preferences;

        this.setTitle(Localization.lang("Select entry type from ID"));
        ViewLoader.view(this)
                .load()
                .setAsDialogPane(this);

        setResultConverter(button -> {
            //The buttonType will always be cancel, even if we pressed one of the entry type buttons
            return type;
        });

        ControlHelper.setAction(lookupButton, this.getDialogPane(), event -> viewModel.runFetcherWorkerForLookUp(lookUpField));

        Button btnLookUp = (Button) this.getDialogPane().lookupButton(lookupButton);
        btnLookUp.textProperty().bind(EasyBind.map(viewModel.searchingProperty(), searching -> (searching) ? Localization.lang("Searching...") : Localization.lang("LookUp")));
        btnLookUp.disableProperty().bind(viewModel.searchingProperty());

        ControlHelper.setAction(generateButton, this.getDialogPane(), event -> viewModel.runFetcherWorker());

        Button btnGenerate = (Button) this.getDialogPane().lookupButton(generateButton);
        btnGenerate.textProperty().bind(EasyBind.map(viewModel.searchingProperty(), searching -> (searching) ? Localization.lang("Adding...") : Localization.lang("Generate")));
        btnGenerate.disableProperty().bind(viewModel.searchingProperty());

    }

    @FXML
    public void initialize() {
        viewModel = new EntryFromIDViewModel(prefs, basePanel, dialogService);

        idTextField.textProperty().bindBidirectional(viewModel.idTextProperty());

        EasyBind.subscribe(viewModel.getFocusAndSelectAllProperty(), evt -> {
            if (evt) {
                idTextField.requestFocus();
                idTextField.selectAll();
            }
        });

        Platform.runLater(() -> idTextField.requestFocus());
    }

    @FXML
    private void runFetcherWorker(Event event) {
        viewModel.runFetcherWorker();
    }

    @FXML
    private void focusTextField(Event event) {
        idTextField.requestFocus();
        idTextField.selectAll();
    }

}
