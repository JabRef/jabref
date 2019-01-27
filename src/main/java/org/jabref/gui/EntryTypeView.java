package org.jabref.gui;

import java.util.Collection;
import java.util.List;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import org.fxmisc.easybind.EasyBind;

/**
 * Dialog that prompts the user to choose a type for an entry.
 * Returns null if canceled.
 */
public class EntryTypeView extends BaseDialog<EntryType> {

    @FXML private ButtonType generateButton;
    @FXML private TextField idTextField;
    @FXML private ComboBox<IdBasedFetcher> idBasedFetchers;
    @FXML private FlowPane biblatexPane;
    @FXML private FlowPane bibTexPane;
    @FXML private FlowPane ieeetranPane;
    @FXML private FlowPane customPane;
    @FXML private TitledPane biblatexTitlePane;
    @FXML private TitledPane bibTexTitlePane;
    @FXML private TitledPane ieeeTranTitlePane;
    @FXML private TitledPane customTitlePane;

    private final BasePanel basePanel;
    private final DialogService dialogService;
    private final JabRefPreferences prefs;

    private EntryType type;
    private EntryTypeViewModel viewModel;

    public EntryTypeView(BasePanel basePanel, DialogService dialogService, JabRefPreferences preferences) {
        this.basePanel = basePanel;
        this.dialogService = dialogService;
        this.prefs = preferences;

        this.setTitle(Localization.lang("Select entry type"));
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(generateButton, this.getDialogPane(), event -> viewModel.runFetcherWorker());

        setResultConverter(button -> {
            //The buttonType will always be cancel, even if we pressed one of the entry type buttons
            return type;
        });

        Button btnGenerate = (Button) this.getDialogPane().lookupButton(generateButton);

        btnGenerate.textProperty().bind(EasyBind.map(viewModel.searchingProperty(), searching -> (searching) ? Localization.lang("Searching...") : Localization.lang("Generate")));
        btnGenerate.disableProperty().bind(viewModel.searchingProperty());

        EasyBind.subscribe(viewModel.searchSuccesfulProperty(), value -> {
            if (value) {
                setEntryTypeForReturnAndClose(null);
            }
        });

    }

    private void addEntriesToPane(FlowPane pane, Collection<? extends EntryType> entries) {

        for (EntryType entryType : entries) {
            Button entryButton = new Button(entryType.getName());
            entryButton.setUserData(entryType);
            entryButton.setOnAction(event -> setEntryTypeForReturnAndClose(entryType));
            pane.getChildren().add(entryButton);
        }
    }

    @FXML
    public void initialize() {
        viewModel = new EntryTypeViewModel(prefs, basePanel, dialogService);

        idBasedFetchers.itemsProperty().bind(viewModel.fetcherItemsProperty());
        idTextField.textProperty().bindBidirectional(viewModel.idTextProperty());
        idBasedFetchers.valueProperty().bindBidirectional(viewModel.selectedItemProperty());

        EasyBind.subscribe(viewModel.getFocusAndSelectAllProperty(), evt -> {
            if (evt) {
                idTextField.requestFocus();
                idTextField.selectAll();
            }
        });

        new ViewModelListCellFactory<IdBasedFetcher>().withText(item -> item.getName()).install(idBasedFetchers);

        //we set the managed property so that they will only be rendered when they are visble so that the Nodes only take the space when visible
        //avoids removing and adding from the scence graph
        bibTexTitlePane.managedProperty().bind(bibTexTitlePane.visibleProperty());
        ieeeTranTitlePane.managedProperty().bind(ieeeTranTitlePane.visibleProperty());
        biblatexTitlePane.managedProperty().bind(biblatexTitlePane.visibleProperty());
        customTitlePane.managedProperty().bind(customTitlePane.visibleProperty());

        if (basePanel.getBibDatabaseContext().isBiblatexMode()) {
            addEntriesToPane(biblatexPane, BiblatexEntryTypes.ALL);

            bibTexTitlePane.setVisible(false);
            ieeeTranTitlePane.setVisible(false);

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX);
            if (customTypes.isEmpty()) {
                customTitlePane.setVisible(false);
            } else {
                addEntriesToPane(customPane, customTypes);
            }

        } else {
            biblatexTitlePane.setVisible(false);
            addEntriesToPane(bibTexPane, BibtexEntryTypes.ALL);
            addEntriesToPane(ieeetranPane, IEEETranEntryTypes.ALL);

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX);
            if (customTypes.isEmpty()) {
                customTitlePane.setVisible(false);
            } else {
                addEntriesToPane(customPane, customTypes);
            }
        }

        Platform.runLater(() -> idTextField.requestFocus());
    }

    public EntryType getChoice() {
        return type;
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

    private void setEntryTypeForReturnAndClose(EntryType entryType) {
        type = entryType;
        viewModel.stopFetching();
        this.close();
    }
}
