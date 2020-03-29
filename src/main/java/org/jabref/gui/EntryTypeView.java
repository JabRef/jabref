package org.jabref.gui;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

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
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;
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
                setEntryTypeForReturnAndClose(Optional.empty());
            }
        });

    }

    private void addEntriesToPane(FlowPane pane, Collection<? extends BibEntryType> entries) {
        for (BibEntryType entryType : entries) {
            Button entryButton = new Button(entryType.getType().getDisplayName());
            entryButton.setUserData(entryType);
            entryButton.setOnAction(event -> setEntryTypeForReturnAndClose(Optional.of(entryType)));
            pane.getChildren().add(entryButton);

            String description = getDescription(entryType);
            if (StringUtil.isNotBlank(description)) {
                Tooltip tooltip = new Tooltip();
                tooltip.setText(description);
                entryButton.setTooltip(tooltip);
            }
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
            addEntriesToPane(biblatexPane, BiblatexEntryTypeDefinitions.ALL);

            bibTexTitlePane.setVisible(false);
            ieeeTranTitlePane.setVisible(false);

            List<BibEntryType> customTypes = Globals.entryTypesManager.getAllCustomTypes(BibDatabaseMode.BIBLATEX);
            if (customTypes.isEmpty()) {
                customTitlePane.setVisible(false);
            } else {
                addEntriesToPane(customPane, customTypes);
            }

        } else {
            biblatexTitlePane.setVisible(false);
            addEntriesToPane(bibTexPane, BibtexEntryTypeDefinitions.ALL);
            addEntriesToPane(ieeetranPane, IEEETranEntryTypeDefinitions.ALL);

            List<BibEntryType> customTypes = Globals.entryTypesManager.getAllCustomTypes(BibDatabaseMode.BIBTEX);
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

    private void setEntryTypeForReturnAndClose(Optional<BibEntryType> entryType) {
        type = entryType.map(BibEntryType::getType).orElse(null);
        viewModel.stopFetching();
        this.close();
    }

    public String getDescription(BibEntryType selectedType) {
        EntryType entryType = selectedType.getType();
        try {
            StandardEntryType entry = (StandardEntryType) entryType;
            switch (entry) {
                case Article:
                    return Localization.lang("An article from a journal or magazine.");
                case Book:
                    return Localization.lang("A book with an explicit publisher.");
                case Booklet:
                    return Localization.lang("A work that is printed and bound, but without a named publisher orsponsoring institution.");
                case Collection:
                    return Localization.lang("A single-volume collection with multiple, self-contained contributions by distinct authors which have their own title. The work as a whole has no overall author but it will usually have an editor.");
                case Conference:
                    return Localization.lang("The same as Inproceedings, included for Scribe compatibility.");
                case InBook:
                    return Localization.lang("A part of a book, which may be a chapter(or section or whatever) and/or a range of pages.");
                case InCollection:
                    return Localization.lang("A part of a book having its own title.");
                case InProceedings:
                    return Localization.lang("An article in a conference proceedings.");
                case Manual:
                    return Localization.lang("Technical documentation.");
                case MastersThesis:
                    return Localization.lang("A Masterβ€™s thesis.");
                case Misc:
                    return Localization.lang("Use this type when nothing else fits");
                case PhdThesis:
                    return Localization.lang("A PhD thesis.");
                case Proceedings:
                    return Localization.lang("The proceedings of a conference.");
                case TechReport:
                    return Localization.lang("A report published by a school or other institution, usually numbered within a series.");
                case Unpublished:
                    return Localization.lang("A document having an author and title, but not formally published.");
                case BookInBook:
                    return Localization.lang("This type is similar to inbook but intended for works originally published as astand-alone book.");
                case InReference:
                    return Localization.lang("An article in a work of reference.");
                case MvBook:
                    return Localization.lang("A multi-volume book.");
                case MvCollection:
                    return Localization.lang("A multi-volume collection.");
                case MvProceedings:
                    return Localization.lang("A multi-volume proceedings entry.");
                case MvReference:
                    return Localization.lang("A multi-volume reference entry. The standard styles will treat this entry typeas an alias for mvcollection.");
                case Online:
                    return Localization.lang("This entry type is intended for sources such as web sites which are intrinsically online resources.");
                case Reference:
                    return Localization.lang("A single-volume work of reference such as an encyclopedia or a dictionary.");
                case Report:
                    return Localization.lang("A technical report, research report, or white paper published by a university or someother institution.");
                case Set:
                    return Localization.lang("An entry set is a group of entries which are cited as a single reference and listed as a single item in the bibliography.");
                case SuppBook:
                    return Localization.lang("Supplemental material in a book. This type is provided for elements such as prefaces, introductions, forewords, afterwords, etc. which often have a generic title only.");
                case SuppCollection:
                    return Localization.lang("Supplemental material in a collection.");
                case SuppPeriodical:
                    return Localization.lang("Supplemental material in a periodical. This type may be useful when referring to items such as regular columns, obituaries, letters to the editor, etc. which only have a generic title.");
                case Thesis:
                    return Localization.lang("A thesis written for an educational institution to satisfy the requirements for a degree.");
                case WWW:
                    return Localization.lang("An alias for online, provided for jurabib compatibility.");
                case Software:
                    return Localization.lang("Computer software. The standard styles will treat this entry type as an alias for misc.");
                case DATESET:
                    return Localization.lang("A data set or a similar collection of (mostly) raw data.");
            }
            return "";
        } catch (Exception e) {
            return "";
        }

    }

}
