// java
package org.jabref.gui.importer.fetcher;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableBooleanValue;
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
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.SearchTextField;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.l10n.Localization;

import com.tobiasdiez.easybind.EasyBind;

public class WebSearchPaneView extends VBox {

    private static final PseudoClass QUERY_INVALID = PseudoClass.getPseudoClass("invalid");

    // helper variables
    private String lastKey = "";
    private long lastKeyTime = 0;
    private int cycleIndex = 0;

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

        getChildren().addAll(fetcherContainer, createQueryField(), createSearchButton());
        this.disableProperty().bind(searchDisabledProperty());
    }

    /**
     * Allows triggering search on pressing enter.
     */
    private void enableEnterToTriggerSearch(TextField query) {
        query.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                viewModel.search();
            }
        });
    }

    private void addQueryValidationHints(TextField query) {
        EasyBind.subscribe(viewModel.queryValidationStatus().validProperty(), valid -> {
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
     * Creates a combo box for selecting a fetcher.
     * Enable letter-jump and cycling in the ComboBox for improved keyboard navigation (\#14083)
     */
    private ComboBox<SearchBasedFetcher> createFetcherComboBox() {
        ComboBox<SearchBasedFetcher> fetchers = new ComboBox<>();
        new ViewModelListCellFactory<SearchBasedFetcher>().withText(SearchBasedFetcher::getName).install(fetchers);
        fetchers.itemsProperty().bind(viewModel.fetchersProperty());
        fetchers.valueProperty().bindBidirectional(viewModel.selectedFetcherProperty());
        fetchers.setMaxWidth(Double.POSITIVE_INFINITY);
        HBox.setHgrow(fetchers, Priority.ALWAYS);

        fetchers.setOnKeyPressed(event -> {
            if (event.getText().length() == 1 && Character.isLetter(event.getText().charAt(0))) {
                String ch = event.getText().toLowerCase();

                long currentTime = System.currentTimeMillis();

                if (ch.equals(lastKey) && (currentTime - lastKeyTime) < 1000) {
                    cycleIndex++;
                } else {
                    cycleIndex = 0;
                }
                lastKey = ch;
                lastKeyTime = currentTime;

                var matching = fetchers.getItems().stream().filter(f -> f.getName().toLowerCase().startsWith(ch)).toList();

                if (!matching.isEmpty()) {
                    fetchers.setValue(matching.get(cycleIndex % matching.size()));
                }
                event.consume();
            }
        });

        return fetchers;
    }

    /**
     * Creates text field for search query.
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
     * Creates button that triggers search.
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
     * Creates help button for currently selected fetcher.
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
     * Creates an observable boolean value that is true if no database is open.
     */
    private ObservableBooleanValue searchDisabledProperty() {
        return Bindings.createBooleanBinding(() -> stateManager.getOpenDatabases().isEmpty(), stateManager.getOpenDatabases());
    }
}
