package org.jabref.gui.importer.fetcher;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableBooleanValue;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchTextField;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;

public class WebSearchPaneView extends VBox {

    private static final PseudoClass QUERY_INVALID = PseudoClass.getPseudoClass("invalid");

    private final WebSearchPaneViewModel viewModel;
    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final StateManager stateManager;

    public WebSearchPaneView(GuiPreferences preferences, DialogService dialogService, StateManager stateManager) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.viewModel = new WebSearchPaneViewModel(preferences, dialogService, stateManager);
        initialize();
    }

    private void initialize() {
        StackPane helpButtonContainer = createHelpButtonContainer();
        HBox fetcherContainer = new HBox(createFetcherComboBox(), helpButtonContainer);

        getChildren().addAll(
                fetcherContainer,
                createQueryField(),
                createIdentifierHint(),
                createSearchButton()
        );
        this.disableProperty().bind(searchDisabledProperty());
    }

    /**
     * Allows triggering search on pressing enter
     */
    private void enableEnterToTriggerSearch(TextField query) {
        query.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.search();
            }
        });
    }

    private void addQueryValidationHints(TextField query) {
        EasyBind.subscribe(viewModel.queryValidationStatus().validProperty(),
                valid -> {
                    if (!valid && viewModel.queryValidationStatus().getHighestMessage().isPresent()) {
                        query.setTooltip(new Tooltip(viewModel.queryValidationStatus().getHighestMessage().get().getMessage()));
                        query.pseudoClassStateChanged(QUERY_INVALID, true);
                    } else {
                        query.setTooltip(null);
                        query.pseudoClassStateChanged(QUERY_INVALID, false);
                    }
                });
    }

    /**
     * Create combo box for selecting fetcher
     */
    private ComboBox<SearchBasedFetcher> createFetcherComboBox() {
        ComboBox<SearchBasedFetcher> fetchers = new ComboBox<>();
        new ViewModelListCellFactory<SearchBasedFetcher>()
                .withText(SearchBasedFetcher::getName)
                .install(fetchers);
        fetchers.itemsProperty().bind(viewModel.fetchersProperty());
        fetchers.valueProperty().bindBidirectional(viewModel.selectedFetcherProperty());
        fetchers.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(fetchers, Priority.ALWAYS);
        return fetchers;
    }

    /**
     * Create text field for search query
     */
    private TextField createQueryField() {
        TextField query = SearchTextField.create(preferences.getKeyBindingRepository());
        viewModel.queryProperty().bind(query.textProperty());
        addQueryValidationHints(query);
        enableEnterToTriggerSearch(query);
        ClipBoardManager.addX11Support(query);
        return query;
    }

    /**
     * Create identifier hint label
     */
    private Label createIdentifierHint() {
        Label identifierHint = new Label();
        identifierHint.getStyleClass().add("identifier-hint");
        identifierHint.visibleProperty().bind(viewModel.identifierDetectedProperty());
        
        // Use EasyBind to create dynamic text binding
        EasyBind.subscribe(viewModel.identifierDetectedProperty(), detected -> {
            if (detected) {
                String identifierType = viewModel.getDetectedIdentifierType();
                identifierHint.setText(Localization.lang("Detected identifier: %0", identifierType));
            } else {
                identifierHint.setText("");
            }
        });
        
        // Also listen to identifier type changes
        EasyBind.subscribe(viewModel.detectedIdentifierTypeProperty(), identifierType -> {
            if (viewModel.isIdentifierDetected()) {
                identifierHint.setText(Localization.lang("Detected identifier: %0", identifierType));
            }
        });
        
        return identifierHint;
    }

    /**
     * Create button that triggers search
     */
    private Button createSearchButton() {
        BooleanExpression importerEnabled = preferences.getImporterPreferences().importerEnabledProperty();
        Button search = new Button(Localization.lang("Search"));
        search.setDefaultButton(false);
        search.setOnAction(event -> viewModel.search());
        search.setMaxWidth(Double.MAX_VALUE);
        search.disableProperty().bind(importerEnabled.not());
        return search;
    }

    /**
     * Creatse help button for currently selected fetcher
     */
    private StackPane createHelpButtonContainer() {
        StackPane helpButtonContainer = new StackPane();
        ActionFactory factory = new ActionFactory();
        EasyBind.subscribe(viewModel.selectedFetcherProperty(), fetcher -> {
            if ((fetcher != null) && fetcher.getHelpPage().isPresent()) {
                Button helpButton = factory.createIconButton(StandardActions.HELP, new HelpAction(fetcher.getHelpPage().get(), dialogService, preferences.getExternalApplicationsPreferences()));
                helpButtonContainer.getChildren().setAll(helpButton);
            } else {
                helpButtonContainer.getChildren().clear();
            }
        });
        return helpButtonContainer;
    }

    /**
     * Creates an observable boolean value that is true if no database is open
     */
    private ObservableBooleanValue searchDisabledProperty() {
        return Bindings.createBooleanBinding(
                () -> stateManager.getOpenDatabases().isEmpty(),
                stateManager.getOpenDatabases()
        );
    }
}