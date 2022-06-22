package org.jabref.gui;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import javafx.stage.Screen;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;

/**
 * Dialog that prompts the user to choose a type for an entry.
 */
public class EntryTypeView extends BaseDialog<EntryType> {

    @Inject StateManager stateManager;

    @FXML private ButtonType generateButton;
    @FXML private TextField idTextField;
    @FXML private ComboBox<IdBasedFetcher> idBasedFetchers;
    @FXML private FlowPane recommendedEntriesPane;
    @FXML private FlowPane otherEntriesPane;
    @FXML private FlowPane customPane;
    @FXML private TitledPane recommendedEntriesTitlePane;
    @FXML private TitledPane otherEntriesTitlePane;
    @FXML private TitledPane customTitlePane;

    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final PreferencesService preferencesService;

    private EntryType type;
    private EntryTypeViewModel viewModel;
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    public EntryTypeView(LibraryTab libraryTab, DialogService dialogService, PreferencesService preferences) {
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.preferencesService = preferences;

        this.setTitle(Localization.lang("Select entry type"));
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(generateButton, this.getDialogPane(), event -> viewModel.runFetcherWorker());

        setResultConverter(button -> {
            // The buttonType will always be "cancel", even if we pressed one of the entry type buttons
            return type;
        });

        Button btnGenerate = (Button) this.getDialogPane().lookupButton(generateButton);

        btnGenerate.textProperty().bind(EasyBind.map(viewModel.searchingProperty(), searching -> (searching) ? Localization.lang("Searching...") : Localization.lang("Generate")));
        btnGenerate.disableProperty().bind(viewModel.idFieldValidationStatus().validProperty().not().or(viewModel.searchingProperty()));

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

            EntryType selectedType = entryType.getType();
            String description = getDescription(selectedType);
            if (StringUtil.isNotBlank(description)) {
                Screen currentScreen = Screen.getPrimary();
                double maxWidth = currentScreen.getBounds().getWidth();
                Tooltip tooltip = new Tooltip(description);
                tooltip.setMaxWidth((maxWidth * 2) / 3);
                tooltip.setWrapText(true);
                entryButton.setTooltip(tooltip);
            }
        }
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());
        viewModel = new EntryTypeViewModel(preferencesService, libraryTab, dialogService, stateManager);

        idBasedFetchers.itemsProperty().bind(viewModel.fetcherItemsProperty());
        idTextField.textProperty().bindBidirectional(viewModel.idTextProperty());
        idBasedFetchers.valueProperty().bindBidirectional(viewModel.selectedItemProperty());

        EasyBind.subscribe(viewModel.getFocusAndSelectAllProperty(), evt -> {
            if (evt) {
                idTextField.requestFocus();
                idTextField.selectAll();
            }
        });

        new ViewModelListCellFactory<IdBasedFetcher>().withText(WebFetcher::getName).install(idBasedFetchers);

        // we set the managed property so that they will only be rendered when they are visble so that the Nodes only take the space when visible
        // avoids removing and adding from the scence graph
        recommendedEntriesTitlePane.managedProperty().bind(recommendedEntriesTitlePane.visibleProperty());
        otherEntriesTitlePane.managedProperty().bind(otherEntriesTitlePane.visibleProperty());
        customTitlePane.managedProperty().bind(customTitlePane.visibleProperty());

        otherEntriesTitlePane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded) {
                this.setHeight(this.getHeight() + otherEntriesPane.getHeight());
            } else {
                this.setHeight(this.getHeight() - otherEntriesPane.getHeight());
            }
        });

        boolean isBiblatexMode = libraryTab.getBibDatabaseContext().isBiblatexMode();
        List<BibEntryType> recommendedEntries;
        List<BibEntryType> otherEntries;
        if (isBiblatexMode) {
            recommendedEntries = BiblatexEntryTypeDefinitions.RECOMMENDED;
            otherEntries = BiblatexEntryTypeDefinitions.ALL
                .stream()
                .filter(e -> !recommendedEntries.contains(e))
                .collect(Collectors.toList());
            otherEntries.addAll(BiblatexSoftwareEntryTypeDefinitions.ALL);
        } else {
            recommendedEntries = BibtexEntryTypeDefinitions.RECOMMENDED;
            otherEntries = BibtexEntryTypeDefinitions.ALL
                .stream()
                .filter(e -> !recommendedEntries.contains(e))
                .collect(Collectors.toList());
            otherEntries.addAll(IEEETranEntryTypeDefinitions.ALL);
        }
        addEntriesToPane(recommendedEntriesPane, recommendedEntries);
        addEntriesToPane(otherEntriesPane, otherEntries);

        BibDatabaseMode customTypeDatabaseMode = isBiblatexMode ? BibDatabaseMode.BIBLATEX : BibDatabaseMode.BIBTEX;
        List<BibEntryType> customTypes = Globals.entryTypesManager.getAllCustomTypes(customTypeDatabaseMode);
        if (customTypes.isEmpty()) {
            customTitlePane.setVisible(false);
        } else {
            addEntriesToPane(customPane, customTypes);
        }

        viewModel.idTextProperty().addListener((obs, oldValue, newValue) ->
                visualizer.initVisualization(viewModel.idFieldValidationStatus(), idTextField, true));

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
        this.stateManager.clearSearchQuery();
        this.close();
    }

    /**
     * The description is originating from biblatex manual chapter 2 Biblatex documentation is favored over the bibtex, since bibtex is a subset of biblatex and biblatex is better documented.
     */
    public static String getDescription(EntryType selectedType) {
        if (selectedType instanceof StandardEntryType) {
            switch ((StandardEntryType) selectedType) {
                case Article -> {
                    return Localization.lang("An article in a journal, magazine, newspaper, or other periodical which forms a self-contained unit with its own title.");
                }
                case Book -> {
                    return Localization.lang("A single-volume book with one or more authors where the authors share credit for the work as a whole.");
                }
                case Booklet -> {
                    return Localization.lang("A book-like work without a formal publisher or sponsoring institution.");
                }
                case Collection -> {
                    return Localization.lang("A single-volume collection with multiple, self-contained contributions by distinct authors which have their own title. The work as a whole has no overall author but it will usually have an editor.");
                }
                case Conference -> {
                    return Localization.lang("A legacy alias for \"InProceedings\".");
                }
                case InBook -> {
                    return Localization.lang("A part of a book which forms a self-contained unit with its own title.");
                }
                case InCollection -> {
                    return Localization.lang("A contribution to a collection which forms a self-contained unit with a distinct author and title.");
                }
                case InProceedings -> {
                    return Localization.lang("An article in a conference proceedings.");
                }
                case Manual -> {
                    return Localization.lang("Technical or other documentation, not necessarily in printed form.");
                }
                case MastersThesis -> {
                    return Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term  Master's thesis.");
                }
                case Misc -> {
                    return Localization.lang("A fallback type for entries which do not fit into any other category.");
                }
                case PhdThesis -> {
                    return Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term PhD thesis.");
                }
                case Proceedings -> {
                    return Localization.lang("A single-volume conference proceedings. This type is very similar to \"Collection\".");
                }
                case TechReport -> {
                    return Localization.lang("Similar to \"Report\" except that the type field is optional and defaults to the localised term technical report.");
                }
                case Unpublished -> {
                    return Localization.lang("A work with an author and a title which has not been formally published, such as a manuscript or the script of a talk.");
                }
                case BookInBook -> {
                    return Localization.lang("This type is similar to \"InBook\" but intended for works originally published as a stand-alone book.");
                }
                case InReference -> {
                    return Localization.lang("An article in a work of reference. This is a more specific variant of the generic \"InCollection\" entry type.");
                }
                case MvBook -> {
                    return Localization.lang("A multi-volume \"Book\".");
                }
                case MvCollection -> {
                    return Localization.lang("A multi-volume \"Collection\".");
                }
                case MvProceedings -> {
                    return Localization.lang("A multi-volume \"Proceedings\" entry.");
                }
                case MvReference -> {
                    return Localization.lang("A multi-volume \"Reference\" entry. The standard styles will treat this entry type as an alias for \"MvCollection\".");
                }
                case Online -> {
                    return Localization.lang("This entry type is intended for sources such as web sites which are intrinsically online resources.");
                }
                case Reference -> {
                    return Localization.lang("A single-volume work of reference such as an encyclopedia or a dictionary.");
                }
                case Report -> {
                    return Localization.lang("A technical report, research report, or white paper published by a university or some other institution.");
                }
                case Set -> {
                    return Localization.lang("An entry set is a group of entries which are cited as a single reference and listed as a single item in the bibliography.");
                }
                case SuppBook -> {
                    return Localization.lang("Supplemental material in a \"Book\". This type is provided for elements such as prefaces, introductions, forewords, afterwords, etc. which often have a generic title only.");
                }
                case SuppCollection -> {
                    return Localization.lang("Supplemental material in a \"Collection\".");
                }
                case SuppPeriodical -> {
                    return Localization.lang("Supplemental material in a \"Periodical\". This type may be useful when referring to items such as regular columns, obituaries, letters to the editor, etc. which only have a generic title.");
                }
                case Thesis -> {
                    return Localization.lang("A thesis written for an educational institution to satisfy the requirements for a degree.");
                }
                case WWW -> {
                    return Localization.lang("An alias for \"Online\", provided for jurabib compatibility.");
                }
                case Software -> {
                    return Localization.lang("Computer software. The standard styles will treat this entry type as an alias for \"Misc\".");
                }
                case Dataset -> {
                    return Localization.lang("A data set or a similar collection of (mostly) raw data.");
                }
                default -> {
                    return "";
                }
            }
        } else {
            return "";
        }
    }
}
