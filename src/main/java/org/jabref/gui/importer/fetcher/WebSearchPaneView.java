package org.jabref.gui.importer.fetcher;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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

    public WebSearchPaneView(PreferencesService preferences, DialogService dialogService, StateManager stateManager) {
        this.preferences = preferences;
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

        // Create help button for currently selected fetcher
        StackPane helpButtonContainer = new StackPane();
        ActionFactory factory = new ActionFactory(preferences.getKeyBindingRepository());
        EasyBind.subscribe(viewModel.selectedFetcherProperty(), fetcher -> {
            if ((fetcher != null) && fetcher.getHelpPage().isPresent()) {
                Button helpButton = factory.createIconButton(StandardActions.HELP, new HelpAction(fetcher.getHelpPage().get()));
                helpButtonContainer.getChildren().setAll(helpButton);
            } else {
                helpButtonContainer.getChildren().clear();
            }
        });
        HBox fetcherContainer = new HBox(fetchers, helpButtonContainer);
        HBox.setHgrow(fetchers, Priority.ALWAYS);

        // Create text field for query input
        TextField query = SearchTextField.create();
        query.getStyleClass().add("searchBar");

        viewModel.queryProperty().bind(query.textProperty());
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

        // Allows triggering search on pressing enter
        query.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.search();
            }
        });

        // Create button that triggers search
        Button search = new Button(Localization.lang("Search"));
        search.setDefaultButton(false);
        search.setOnAction(event -> viewModel.search());

        setAlignment(Pos.CENTER);
        getChildren().addAll(fetcherContainer, query, search);
    }
}
