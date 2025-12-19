package org.jabref.gui.newentry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.TilePane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.fieldeditors.EditorValidator;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.RfcFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.model.entry.identifier.RFC;
import org.jabref.model.entry.identifier.SSRN;
import org.jabref.model.entry.types.BiblatexAPAEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.BiblatexNonStandardEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import jakarta.inject.Inject;
import org.jspecify.annotations.NonNull;

public class NewEntryView extends BaseDialog<BibEntry> {
    private static final String BIBTEX_REGEX = "^@([A-Za-z]+)\\{,";
    private static final String LINE_BREAK = "\n";

    private NewEntryViewModel viewModel;

    private final NewEntryDialogTab initialApproach;
    private NewEntryDialogTab currentApproach;

    private final GuiPreferences guiPreferences;
    private final NewEntryPreferences preferences;
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    @Inject private StateManager stateManager;
    @Inject private TaskExecutor taskExecutor;
    @Inject private AiService aiService;
    @Inject private FileUpdateMonitor fileUpdateMonitor;

    private final ControlsFxVisualizer visualizer;

    @FXML private ButtonType generateButtonType;
    private Button generateButton;

    @FXML private TabPane tabs;
    @FXML private Tab tabAddEntry;
    @FXML private Tab tabLookupIdentifier;
    @FXML private Tab tabInterpretCitations;
    @FXML private Tab tabSpecifyBibtex;

    @FXML private TitledPane entryRecommendedTitle;
    @FXML private TilePane entryRecommended;
    @FXML private TitledPane entryOtherTitle;
    @FXML private TilePane entryOther;
    @FXML private TitledPane entryCustomTitle;
    @FXML private TilePane entryCustom;
    @FXML private TitledPane entryNonStandardTitle;
    @FXML private TilePane entryNonStandard;

    @FXML private TextField idText;
    @FXML private Tooltip idTextTooltip;
    @FXML private Hyperlink idJumpLink;
    @FXML private RadioButton idLookupGuess;
    @FXML private RadioButton idLookupSpecify;
    @FXML private ComboBox<IdBasedFetcher> idFetcher;
    @FXML private Label idErrorInvalidText;
    @FXML private Label idErrorInvalidFetcher;

    @FXML private TextArea interpretText;
    @FXML private ComboBox<PlainCitationParserChoice> interpretParser;

    @FXML private TextArea bibtexText;

    private BibEntry result;

    public NewEntryView(NewEntryDialogTab initialApproach, GuiPreferences preferences, LibraryTab libraryTab, DialogService dialogService) {
        this.initialApproach = initialApproach;
        this.currentApproach = initialApproach;

        this.guiPreferences = preferences;
        this.preferences = preferences.getNewEntryPreferences();
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;

        visualizer = new ControlsFxVisualizer();
        this.setTitle(Localization.lang("New Entry"));
        ViewLoader.view(this).load().setAsDialogPane(this);

        generateButton = (Button) this.getDialogPane().lookupButton(generateButtonType);
        generateButton.getStyleClass().add("customGenerateButton");

        final Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setHeight(650);
        stage.setWidth(931);
        stage.setMinHeight(300);
        stage.setMinWidth(400);

        ControlHelper.setAction(generateButtonType, getDialogPane(), _ -> execute());
        setOnCloseRequest(_ -> cancel());
        setResultConverter(_ -> result);

        getDialogPane().disableProperty().bind(viewModel.executingProperty());

        finalizeTabs();
        tabs.requestFocus();
    }

    private void finalizeTabs() {
        NewEntryDialogTab approach = initialApproach;
        if (approach == null) {
            final String clipboardText = ClipBoardManager.getContents().trim();
            if (!StringUtil.isBlank(clipboardText)) {
                Optional<Identifier> identifier = Identifier.from(clipboardText);
                if (identifier.isPresent()) {
                    approach = NewEntryDialogTab.ENTER_IDENTIFIER;
                    interpretText.setText(clipboardText);
                    interpretText.selectAll();
                } else if (clipboardText.split(LINE_BREAK)[0].matches(BIBTEX_REGEX)) {
                    approach = NewEntryDialogTab.SPECIFY_BIBTEX;
                } else {
                    approach = preferences.getLatestApproach();
                }
            } else {
                approach = preferences.getLatestApproach();
            }
        }

        switch (approach) {
            case NewEntryDialogTab.CHOOSE_ENTRY_TYPE:
                tabs.getSelectionModel().select(tabAddEntry);
                switchAddEntry();
                break;
            case NewEntryDialogTab.ENTER_IDENTIFIER:
                tabs.getSelectionModel().select(tabLookupIdentifier);
                switchLookupIdentifier();
                break;
            case NewEntryDialogTab.INTERPRET_CITATIONS:
                tabs.getSelectionModel().select(tabInterpretCitations);
                switchInterpretCitations();
                break;
            case NewEntryDialogTab.SPECIFY_BIBTEX:
                tabs.getSelectionModel().select(tabSpecifyBibtex);
                switchSpecifyBibtex();
                break;
        }

        tabAddEntry.setOnSelectionChanged(_ -> switchAddEntry());
        tabLookupIdentifier.setOnSelectionChanged(_ -> switchLookupIdentifier());
        tabInterpretCitations.setOnSelectionChanged(_ -> switchInterpretCitations());
        tabSpecifyBibtex.setOnSelectionChanged(_ -> switchSpecifyBibtex());
    }

    @FXML
    public void initialize() {
        viewModel = new NewEntryViewModel(guiPreferences, libraryTab, dialogService, stateManager, (UiTaskExecutor) taskExecutor, aiService, fileUpdateMonitor);

        visualizer.setDecoration(new IconValidationDecorator());

        EasyBind.subscribe(
                viewModel.executedSuccessfullyProperty(),
                succeeded -> {
                    if (succeeded) {
                        onSuccessfulExecution();
                    }
                });

        initializeAddEntry();
        initializeLookupIdentifier();
        initializeInterpretCitations();
        initializeSpecifyBibTeX();
    }

    private void initializeAddEntry() {
        entryRecommendedTitle.managedProperty().bind(entryRecommendedTitle.visibleProperty());
        entryRecommendedTitle.expandedProperty().bindBidirectional(preferences.typesRecommendedExpandedProperty());
        entryRecommended.managedProperty().bind(entryRecommended.visibleProperty());

        entryOtherTitle.managedProperty().bind(entryOtherTitle.visibleProperty());
        entryOtherTitle.expandedProperty().bindBidirectional(preferences.typesOtherExpandedProperty());
        entryOther.managedProperty().bind(entryOther.visibleProperty());

        entryCustomTitle.managedProperty().bind(entryCustomTitle.visibleProperty());
        entryCustomTitle.expandedProperty().bindBidirectional(preferences.typesCustomExpandedProperty());
        entryCustom.managedProperty().bind(entryCustom.visibleProperty());

        entryNonStandardTitle.managedProperty().bind(entryNonStandardTitle.visibleProperty());
        entryNonStandard.managedProperty().bind(entryNonStandard.visibleProperty());

        final boolean isBiblatexMode = libraryTab.getBibDatabaseContext().isBiblatexMode();

        List<BibEntryType> recommendedEntries;
        List<BibEntryType> otherEntries;
        if (isBiblatexMode) {
            recommendedEntries = BiblatexEntryTypeDefinitions.RECOMMENDED;
            otherEntries = new ArrayList<>(BiblatexEntryTypeDefinitions.ALL);
            otherEntries.removeAll(recommendedEntries);
            otherEntries.addAll(BiblatexSoftwareEntryTypeDefinitions.ALL);
            otherEntries.addAll(BiblatexAPAEntryTypeDefinitions.ALL);
        } else {
            recommendedEntries = BibtexEntryTypeDefinitions.RECOMMENDED;
            otherEntries = new ArrayList<>(BiblatexEntryTypeDefinitions.ALL);
            otherEntries.removeAll(recommendedEntries);
            otherEntries.addAll(IEEETranEntryTypeDefinitions.ALL);
        }
        addEntriesToPane(entryRecommended, recommendedEntries);
        addEntriesToPane(entryOther, otherEntries);

        final BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
        final BibDatabaseMode customTypesDatabaseMode = isBiblatexMode ? BibDatabaseMode.BIBLATEX : BibDatabaseMode.BIBTEX;
        final List<BibEntryType> customEntries = entryTypesManager.getAllCustomTypes(customTypesDatabaseMode);
        if (customEntries.isEmpty()) {
            entryCustomTitle.setVisible(false);
        } else {
            addEntriesToPane(entryCustom, customEntries);
        }

        if (isBiblatexMode) {
            addEntriesToPane(entryNonStandard, BiblatexNonStandardEntryTypeDefinitions.ALL);
        } else {
            entryNonStandardTitle.setVisible(false);
        }
    }

    private void initializeLookupIdentifier() {
        // TODO: It would be nice if this was a `TextArea`, so that users could enter multiple IDs at once. The view
        //       model would then iterate through all non-blank lines, passing each of them through the specified lookup
        //       method (each automatically independently, or all through the same fetcher).
        idText.setPromptText(Localization.lang("Enter the reference identifier to search for."));
        idText.textProperty().bindBidirectional(viewModel.idTextProperty());

        ToggleGroup toggleGroup = new ToggleGroup();
        idLookupGuess.setToggleGroup(toggleGroup);
        idLookupSpecify.setToggleGroup(toggleGroup);

        if (preferences.getIdLookupGuessing()) {
            idLookupGuess.selectedProperty().set(true);
        } else {
            idLookupSpecify.selectedProperty().set(true);
        }

        viewModel.populateDOICache();

        // [impl->req~newentry.clipboard.autofocus~1]
        Optional<Identifier> validClipboardId = extractValidIdentifierFromClipboard();
        if (validClipboardId.isPresent()) {
            viewModel.duplicateDoiValidatorStatus().validProperty().addListener((_, _, isValid) -> {
                if (isValid) {
                    Tooltip.install(idText, idTextTooltip);
                } else {
                    Tooltip.uninstall(idText, idTextTooltip);
                }
            });

            idText.setText(ClipBoardManager.getContents().trim());
            idText.selectAll();

            Identifier id = validClipboardId.get();
            Platform.runLater(() -> {
                idLookupSpecify.setSelected(true);
                fetcherForIdentifier(id).ifPresent(idFetcher::setValue);
            });
        } else {
            Platform.runLater(() -> idLookupGuess.setSelected(true));
        }

        idLookupGuess.selectedProperty().addListener((_, _, newValue) -> {
            preferences.setIdLookupGuessing(newValue);
            // When switching to auto-detect mode, detect identifier type from current text
            if (newValue && idText.getText() != null && !idText.getText().trim().isEmpty()) {
                Optional<Identifier> identifier = Identifier.from(idText.getText().trim());
                if (identifier.isPresent() && isValidIdentifier(identifier.get())) {
                    fetcherForIdentifier(identifier.get()).ifPresent(idFetcher::setValue);
                }
            }
        });

        idFetcher.itemsProperty().bind(viewModel.idFetchersProperty());
        new ViewModelListCellFactory<IdBasedFetcher>().withText(WebFetcher::getName).install(idFetcher);
        idFetcher.disableProperty().bind(idLookupSpecify.selectedProperty().not());
        idFetcher.valueProperty().bindBidirectional(viewModel.idFetcherProperty());
        IdBasedFetcher initialFetcher = fetcherFromName(preferences.getLatestIdFetcher(), idFetcher.getItems());
        if (initialFetcher == null) {
            final IdBasedFetcher defaultFetcher = new DoiFetcher(guiPreferences.getImportFormatPreferences());
            initialFetcher = fetcherFromName(defaultFetcher.getName(), idFetcher.getItems());
        }
        idFetcher.setValue(initialFetcher);
        idFetcher.setOnAction(_ -> preferences.setLatestIdFetcher(idFetcher.getValue().getName()));

        // Auto-detect identifier type when typing in the identifier field
        // Only works when "Automatically determine identifier type" is selected
        idText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (idLookupGuess.isSelected() && newValue != null && !newValue.trim().isEmpty()) {
                Optional<Identifier> identifier = Identifier.from(newValue.trim());
                if (identifier.isPresent() && isValidIdentifier(identifier.get())) {
                    fetcherForIdentifier(identifier.get()).ifPresent(idFetcher::setValue);
                }
            }
        });

        idJumpLink.visibleProperty().bind(viewModel.duplicateDoiValidatorStatus().validProperty().not());
        idErrorInvalidText.visibleProperty().bind(viewModel.idTextValidatorProperty().not());
        idErrorInvalidText.managedProperty().bind(viewModel.idTextValidatorProperty().not());
        idErrorInvalidFetcher.visibleProperty().bind(idLookupSpecify.selectedProperty().and(viewModel.idFetcherValidatorProperty().not()));

        idJumpLink.setOnAction(_ -> libraryTab.showAndEdit(viewModel.getDuplicateEntry()));

        TextInputControl textInput = idText;
        EditorValidator validator = new EditorValidator(this.guiPreferences);
        validator.configureValidation(viewModel.duplicateDoiValidatorStatus(), textInput);
    }

    private void initializeInterpretCitations() {
        interpretText.textProperty().bindBidirectional(viewModel.interpretTextProperty());
        final String clipboardText = ClipBoardManager.getContents().trim();
        if (!StringUtil.isBlank(clipboardText)) {
            interpretText.setText(clipboardText);
            interpretText.selectAll();
        }

        interpretParser.itemsProperty().bind(viewModel.interpretParsersProperty());
        new ViewModelListCellFactory<PlainCitationParserChoice>().withText(PlainCitationParserChoice::getLocalizedName).install(interpretParser);
        interpretParser.valueProperty().bindBidirectional(viewModel.interpretParserProperty());
        PlainCitationParserChoice initialParser = parserFromName(preferences.getLatestInterpretParser(), interpretParser.getItems());
        if (initialParser == null) {
            final PlainCitationParserChoice defaultParser = PlainCitationParserChoice.RULE_BASED_GENERAL;
            initialParser = parserFromName(defaultParser.getLocalizedName(), interpretParser.getItems());
        }
        interpretParser.setValue(initialParser);
        interpretParser.setOnAction(_ -> preferences.setLatestInterpretParser(interpretParser.getValue().getLocalizedName()));
    }

    private void initializeSpecifyBibTeX() {
        bibtexText.textProperty().bindBidirectional(viewModel.bibtexTextProperty());
        final String clipboardText = ClipBoardManager.getContents().trim();
        if (!StringUtil.isBlank(clipboardText)) {
            // TODO: Better validation would be nice here, so clipboard text is only copied over if it matches a
            // supported Bib(La)TeX source format.
            bibtexText.setText(clipboardText);
            bibtexText.selectAll();
        }
    }

    @FXML
    private void switchAddEntry() {
        if (!tabAddEntry.isSelected()) {
            return;
        }

        currentApproach = NewEntryDialogTab.CHOOSE_ENTRY_TYPE;
        preferences.setLatestApproach(NewEntryDialogTab.CHOOSE_ENTRY_TYPE);

        if (generateButton != null) {
            generateButton.disableProperty().unbind();
            generateButton.setDisable(true);
            generateButton.setText(Localization.lang("Select"));
        }
    }

    @FXML
    private void switchLookupIdentifier() {
        if (!tabLookupIdentifier.isSelected()) {
            return;
        }

        currentApproach = NewEntryDialogTab.ENTER_IDENTIFIER;
        preferences.setLatestApproach(NewEntryDialogTab.ENTER_IDENTIFIER);

        if (idText != null) {
            Platform.runLater(() -> idText.requestFocus());
        }

        if (generateButton != null) {
            generateButton.disableProperty().bind(idErrorInvalidText.visibleProperty().or(idErrorInvalidFetcher.visibleProperty()));
            generateButton.setText(Localization.lang("Search"));
        }
    }

    @FXML
    private void switchInterpretCitations() {
        if (!tabInterpretCitations.isSelected()) {
            return;
        }

        currentApproach = NewEntryDialogTab.INTERPRET_CITATIONS;
        preferences.setLatestApproach(NewEntryDialogTab.INTERPRET_CITATIONS);

        if (interpretText != null) {
            Platform.runLater(() -> interpretText.requestFocus());
        }

        if (generateButton != null) {
            generateButton.disableProperty().bind(viewModel.interpretTextValidatorProperty().not());
            generateButton.setText(Localization.lang("Parse"));
        }
    }

    @FXML
    private void switchSpecifyBibtex() {
        if (!tabSpecifyBibtex.isSelected()) {
            return;
        }

        currentApproach = NewEntryDialogTab.SPECIFY_BIBTEX;
        preferences.setLatestApproach(NewEntryDialogTab.SPECIFY_BIBTEX);

        if (bibtexText != null) {
            Platform.runLater(() -> bibtexText.requestFocus());
        }

        if (generateButton != null) {
            generateButton.disableProperty().bind(viewModel.bibtexTextValidatorProperty().not());
            generateButton.setText(Localization.lang("Create"));
        }
    }

    private void onEntryTypeSelected(EntryType type) {
        preferences.setLatestImmediateType(type);
        result = new BibEntry(type);
        this.close();
    }

    private void onSuccessfulExecution() {
        viewModel.cancel();
        stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).set(Optional.empty());
        this.close();
    }

    private void execute() {
        // :TODO: These button text changes aren't actually visible, due to the UI thread not being able to perform the
        // update before the button text is reset. The `viewModel.execute*()` and `switch*()` calls could be wrapped in
        // a `Platform.runLater(...)` which would probably fix this.
        switch (currentApproach) {
            case NewEntryDialogTab.CHOOSE_ENTRY_TYPE:
                // We do nothing here.
                break;
            case NewEntryDialogTab.ENTER_IDENTIFIER:
                generateButton.setText(Localization.lang("Searching..."));
                viewModel.executeLookupIdentifier(idLookupGuess.isSelected());
                switchLookupIdentifier();
                break;
            case NewEntryDialogTab.INTERPRET_CITATIONS:
                generateButton.setText(Localization.lang("Parsing..."));
                viewModel.executeInterpretCitations();
                switchInterpretCitations();
                break;
            case NewEntryDialogTab.SPECIFY_BIBTEX:
                generateButton.setText(Localization.lang("Parsing..."));
                viewModel.executeSpecifyBibtex();
                switchSpecifyBibtex();
                break;
        }
    }

    private void cancel() {
        viewModel.cancel();
    }

    private void addEntriesToPane(TilePane pane, Collection<? extends BibEntryType> entries) {
        final double maxTooltipWidth = (2.0 / 3.0) * Screen.getPrimary().getBounds().getWidth();

        for (BibEntryType entry : entries) {
            final EntryType type = entry.getType();

            final Button button = new Button(type.getDisplayName());
            button.setMinWidth(Button.USE_PREF_SIZE);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setUserData(entry);
            button.setOnAction(_ -> onEntryTypeSelected(type));

            final String description = descriptionOfEntryType(type);
            if (description != null) {
                final Tooltip tooltip = new Tooltip(description);
                tooltip.setMaxWidth(maxTooltipWidth);
                tooltip.setWrapText(true);
                button.setTooltip(tooltip);
            }

            pane.getChildren().add(button);
        }
    }

    private static String descriptionOfEntryType(EntryType type) {
        if (type instanceof StandardEntryType entryType) {
            return descriptionOfStandardEntryType(entryType);
        }
        if (type instanceof BiblatexNonStandardEntryType entryType) {
            return descriptionOfNonStandardEntryType(entryType);
        }
        return null;
    }

    private static String descriptionOfStandardEntryType(StandardEntryType type) {
        // These descriptions are taken from subsection 2.1 of the biblatex package documentation.
        // Biblatex is a superset of bibtex, with more elaborate descriptions, so its documentation is preferred.
        // See [https://mirrors.ibiblio.org/pub/mirrors/CTAN/macros/latex/contrib/biblatex/doc/biblatex.pdf].
        return switch (type) {
            case Article ->
                    Localization.lang("An article in a journal, magazine, newspaper, or other periodical which forms a self-contained unit with its own title.");
            case Book ->
                    Localization.lang("A single-volume book with one or more authors where the authors share credit for the work as a whole.");
            case Booklet ->
                    Localization.lang("A book-like work without a formal publisher or sponsoring institution.");
            case Collection ->
                    Localization.lang("A single-volume collection with multiple, self-contained contributions by distinct authors which have their own title. The work as a whole has no overall author but it will usually have an editor.");
            case Conference ->
                    Localization.lang("A legacy alias for \"InProceedings\".");
            case InBook ->
                    Localization.lang("A part of a book which forms a self-contained unit with its own title.");
            case InCollection ->
                    Localization.lang("A contribution to a collection which forms a self-contained unit with a distinct author and title.");
            case InProceedings ->
                    Localization.lang("An article in a conference proceedings.");
            case Manual ->
                    Localization.lang("Technical or other documentation, not necessarily in printed form.");
            case MastersThesis ->
                    Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term  Master's thesis.");
            case Misc ->
                    Localization.lang("A fallback type for entries which do not fit into any other category.");
            case PhdThesis ->
                    Localization.lang("Similar to \"Thesis\" except that the type field is optional and defaults to the localised term PhD thesis.");
            case Proceedings ->
                    Localization.lang("A single-volume conference proceedings. This type is very similar to \"Collection\".");
            case TechReport ->
                    Localization.lang("Similar to \"Report\" except that the type field is optional and defaults to the localised term technical report.");
            case Unpublished ->
                    Localization.lang("A work with an author and a title which has not been formally published, such as a manuscript or the script of a talk.");
            case BookInBook ->
                    Localization.lang("This type is similar to \"InBook\" but intended for works originally published as a stand-alone book.");
            case InReference ->
                    Localization.lang("An article in a work of reference. This is a more specific variant of the generic \"InCollection\" entry type.");
            case MvBook ->
                    Localization.lang("A multi-volume \"Book\".");
            case MvCollection ->
                    Localization.lang("A multi-volume \"Collection\".");
            case MvProceedings ->
                    Localization.lang("A multi-volume \"Proceedings\" entry.");
            case MvReference ->
                    Localization.lang("A multi-volume \"Reference\" entry. The standard styles will treat this entry type as an alias for \"MvCollection\".");
            case Online ->
                    Localization.lang("This entry type is intended for sources such as web sites which are intrinsically online resources.");
            case Reference ->
                    Localization.lang("A single-volume work of reference such as an encyclopedia or a dictionary.");
            case Report ->
                    Localization.lang("A technical report, research report, or white paper published by a university or some other institution.");
            case Set ->
                    Localization.lang("An entry set is a group of entries which are cited as a single reference and listed as a single item in the bibliography.");
            case SuppBook ->
                    Localization.lang("Supplemental material in a \"Book\". This type is provided for elements such as prefaces, introductions, forewords, afterwords, etc. which often have a generic title only.");
            case SuppCollection ->
                    Localization.lang("Supplemental material in a \"Collection\".");
            case SuppPeriodical ->
                    Localization.lang("Supplemental material in a \"Periodical\". This type may be useful when referring to items such as regular columns, obituaries, letters to the editor, etc. which only have a generic title.");
            case Thesis ->
                    Localization.lang("A thesis written for an educational institution to satisfy the requirements for a degree.");
            case WWW ->
                    Localization.lang("An alias for \"Online\", provided for jurabib compatibility.");
            case Software ->
                    Localization.lang("Computer software. The standard styles will treat this entry type as an alias for \"Misc\".");
            case Dataset ->
                    Localization.lang("A data set or a similar collection of (mostly) raw data.");
        };
    }

    private static String descriptionOfNonStandardEntryType(BiblatexNonStandardEntryType type) {
        // These descriptions are taken from subsection 2.1.3 of the biblatex package documentation.
        // Non-standard Types (BibLaTeX only) - these use the @misc driver in standard bibliography styles.
        // See [https://mirrors.ibiblio.org/pub/mirrors/CTAN/macros/latex/contrib/biblatex/doc/biblatex.pdf].
        return switch (type) {
            case Artwork ->
                    Localization.lang("Works of the visual arts such as paintings, sculpture, and installations.");
            case Audio ->
                    Localization.lang("Audio recordings, typically on audio cd, dvd, audio cassette, or similar media.");
            case Bibnote ->
                    Localization.lang("This special entry type is not meant to be used in the bib file like other types. It is provided for third-party packages which merge notes into the bibliography.");
            case Commentary ->
                    Localization.lang("Commentaries which have a status different from regular books, such as legal commentaries.");
            case Image ->
                    Localization.lang("Images, pictures, photographs, and similar media.");
            case Jurisdiction ->
                    Localization.lang("Court decisions, court recordings, and similar things.");
            case Legislation ->
                    Localization.lang("Laws, bills, legislative proposals, and similar things.");
            case Legal ->
                    Localization.lang("Legal documents such as treaties.");
            case Letter ->
                    Localization.lang("Personal correspondence such as letters, emails, memoranda, etc.");
            case Movie ->
                    Localization.lang("Motion pictures.");
            case Music ->
                    Localization.lang("Musical recordings. This is a more specific variant of \"Audio\".");
            case Performance ->
                    Localization.lang("Musical and theatrical performances as well as other works of the performing arts. This type refers to the event as opposed to a recording, a score, or a printed play.");
            case Review ->
                    Localization.lang("Reviews of some other work. This is a more specific variant of the \"Article\" type.");
            case Standard ->
                    Localization.lang("National and international standards issued by a standards body such as the International Organization for Standardization.");
            case Video ->
                    Localization.lang("Audiovisual recordings, typically on dvd, vhs cassette, or similar media.");
        };
    }

    private static IdBasedFetcher fetcherFromName(String fetcherName, List<IdBasedFetcher> fetchers) {
        for (IdBasedFetcher fetcher : fetchers) {
            if (fetcher.getName().equals(fetcherName)) {
                return fetcher;
            }
        }
        return null;
    }

    private static @NonNull PlainCitationParserChoice parserFromName(String parserName, List<PlainCitationParserChoice> parsers) {
        for (PlainCitationParserChoice parser : parsers) {
            if (parser.getLocalizedName().equals(parserName)) {
                return parser;
            }
        }
        // If nothing could be mapped, return the default - set at {@link org.jabref.gui.preferences.JabRefGuiPreferences.JabRefGuiPreferences}
        return PlainCitationParserChoice.RULE_BASED_GENERAL;
    }

    /**
     * Validates an identifier. DOI and ISBN identifiers are validated,
     * other identifier types are considered valid by default.
     *
     * @param id the identifier to validate
     * @return true if the identifier is valid, false otherwise
     */
    private boolean isValidIdentifier(Identifier id) {
        return switch (id) {
            case DOI doi -> DOI.isValid(doi.asString());
            case ISBN isbn -> isbn.isValid();
            default -> true;
        };
    }

    private Optional<Identifier> extractValidIdentifierFromClipboard() {
        String clipboardText = ClipBoardManager.getContents().trim();

        if (!StringUtil.isBlank(clipboardText) && !clipboardText.contains("\n")) {
            Optional<Identifier> identifier = Identifier.from(clipboardText);
            if (identifier.isPresent()) {
                Identifier id = identifier.get();
                if (isValidIdentifier(id)) {
                    return Optional.of(id);
                }
            }
        }

        return Optional.empty();
    }

    private Optional<IdBasedFetcher> fetcherForIdentifier(Identifier id) {
        for (IdBasedFetcher fetcher : idFetcher.getItems()) {
            if ((id instanceof DOI && fetcher instanceof DoiFetcher) ||
                    (id instanceof ISBN && (fetcher instanceof IsbnFetcher) ||
                            (id instanceof ArXivIdentifier && fetcher instanceof ArXivFetcher) ||
                            (id instanceof RFC && fetcher instanceof RfcFetcher) ||
                            (id instanceof SSRN && fetcher instanceof DoiFetcher))) {
                return Optional.of(fetcher);
            }
        }
        return Optional.empty();
    }
}
