package org.jabref.gui.importer.fetcher;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.search.SearchTextField;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.tobiasdiez.easybind.EasyBind;

public class WebSearchPane extends SidePaneComponent {

    private final JabRefPreferences preferences;
    private final WebSearchPaneViewModel viewModel;

    public WebSearchPane(SidePaneManager sidePaneManager, JabRefPreferences preferences, JabRefFrame frame) {
        super(sidePaneManager, IconTheme.JabRefIcons.WWW, Localization.lang("Web search"));
        this.preferences = preferences;
        this.viewModel = new WebSearchPaneViewModel(preferences.getImportFormatPreferences(), frame, preferences, frame.getDialogService());
    }

    @Override
    public Action getToggleAction() {
        return StandardActions.TOGGLE_WEB_SEARCH;
    }

    @Override
    protected Node createContentPane() {
        // Setup combo box for fetchers
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
        query.textProperty().addListener((observable, oldValue, newValue) -> viewModel.validateQueryStringAndGiveColorFeedback(query, newValue));
        query.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                viewModel.validateQueryStringAndGiveColorFeedback(query, query.getText());
            } else {
                viewModel.setPseudoClassToValid(query);
            }
        });

        viewModel.queryProperty().bind(query.textProperty());

        // Create button that triggers search
        Button search = new Button(Localization.lang("Search"));
        search.setDefaultButton(false);
        search.setOnAction(event -> viewModel.search());

        // Put everything together
        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.getChildren().addAll(fetcherContainer, query, search);
        return container;
    }

    @Override
    public SidePaneType getType() {
        return SidePaneType.WEB_SEARCH;
    }

    @Override
    public void beforeClosing() {
        preferences.putBoolean(JabRefPreferences.WEB_SEARCH_VISIBLE, Boolean.FALSE);
    }

    @Override
    public void afterOpening() {
        preferences.putBoolean(JabRefPreferences.WEB_SEARCH_VISIBLE, Boolean.TRUE);
    }

    @Override
    public Priority getResizePolicy() {
        return Priority.NEVER;
    }
}
