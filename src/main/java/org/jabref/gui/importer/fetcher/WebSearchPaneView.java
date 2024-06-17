package org.jabref.gui.importer.fetcher;

import javafx.beans.binding.BooleanExpression;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import org.jabref.gui.search.SearchTextField;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class WebSearchPaneView extends VBox {

    private static final PseudoClass QUERY_INVALID = PseudoClass.getPseudoClass("invalid");

    private final WebSearchPaneViewModel viewModel;
    private final PreferencesService preferences;
    private final DialogService dialogService;

    public WebSearchPaneView(PreferencesService preferences, DialogService dialogService, StateManager stateManager) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.viewModel = new WebSearchPaneViewModel(preferences, dialogService, stateManager);
        initialize();
    }

    private void initialize() {
        ComboBox<SearchBasedFetcher> fetchers = new ComboBox<>();
        new ViewModelListCellFactory<SearchBasedFetcher>()
                .withText(SearchBasedFetcher::getName)
                .install(fetchers);
        fetchers.itemsProperty().bind(viewModel.fetchersProperty());
        fetchers.valueProperty().bindBidirectional(viewModel.selectedFetcherProperty());
        fetchers.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(fetchers, Priority.ALWAYS);

        StackPane helpButtonContainer = createHelpButtonContainer();
        HBox fetcherContainer = new HBox(fetchers, helpButtonContainer);
        TextField query = SearchTextField.create(preferences.getKeyBindingRepository());
        getChildren().addAll(fetcherContainer, query, createSearchButton());

        viewModel.queryProperty().bind(query.textProperty());

        addQueryValidationHints(query);

        enableEnterToTriggerSearch(query);

        ClipBoardManager.addX11Support(query);
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
                Button helpButton = factory.createIconButton(StandardActions.HELP, new HelpAction(fetcher.getHelpPage().get(), dialogService, preferences.getFilePreferences()));
                helpButtonContainer.getChildren().setAll(helpButton);
            } else {
                helpButtonContainer.getChildren().clear();
            }
        });
        return helpButtonContainer;
    }
}
