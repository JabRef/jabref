package org.jabref.gui.search;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.undo.UndoManager;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.textfield.CustomTextField;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class SearchResultDialogSearchBar extends HBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchBar.class);

    private static final int SEARCH_DELAY = 400;
    private static final PseudoClass CLASS_NO_RESULTS = PseudoClass.getPseudoClass("emptyResult");
    private static final PseudoClass CLASS_RESULTS_FOUND = PseudoClass.getPseudoClass("emptyResult");

    private final CustomTextField searchField = SearchTextField.create();
    private final ToggleButton caseSensitiveButton;
    private final ToggleButton regularExpressionButton;
    private final ToggleButton fulltextButton;
    private final Tooltip searchFieldTooltip = new Tooltip();
    private final Label currentResults = new Label("");

    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final Validator regexValidator;
    private final SearchPreferences searchPreferences;

    public SearchResultDialogSearchBar(LibraryTabContainer tabContainer,
                           StateManager stateManager,
                           PreferencesService preferencesService,
                           UndoManager undoManager) {
        super();
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.searchPreferences = preferencesService.getSearchPreferences();

        searchField.disableProperty().bind(needsDatabase(stateManager).not());

        // fits the standard "found x entries"-message thus hinders the searchbar to jump around while searching if the tabContainer width is too small
        currentResults.setPrefWidth(150);

        searchField.setTooltip(searchFieldTooltip);
        searchFieldTooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        searchFieldTooltip.setMaxHeight(10);
        updateHintVisibility();

        KeyBindingRepository keyBindingRepository = preferencesService.getKeyBindingRepository();
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                if (keyBinding.get() == KeyBinding.CLOSE) {
                    // Clear search field
                    searchField.setText("");
                    event.consume();
                }
            }
        });

        searchField.setContextMenu(SearchFieldRightClickMenu.create(
                keyBindingRepository,
                stateManager,
                searchField,
                tabContainer,
                undoManager));

        ObservableList<String> search = stateManager.getWholeSearchHistory();
        search.addListener((ListChangeListener.Change<? extends String> change) -> {
            searchField.setContextMenu(SearchFieldRightClickMenu.create(
                    keyBindingRepository,
                    stateManager,
                    searchField,
                    tabContainer,
                    undoManager));
        });

        ClipBoardManager.addX11Support(searchField);

        regularExpressionButton = IconTheme.JabRefIcons.REG_EX.asToggleButton();
        caseSensitiveButton = IconTheme.JabRefIcons.CASE_SENSITIVE.asToggleButton();
        fulltextButton = IconTheme.JabRefIcons.FULLTEXT.asToggleButton();

        initSearchModifierButtons();

        BooleanBinding focusedOrActive = searchField.focusedProperty()
                                                    .or(regularExpressionButton.focusedProperty())
                                                    .or(caseSensitiveButton.focusedProperty())
                                                    .or(fulltextButton.focusedProperty())
                                                    .or(searchField.textProperty()
                                                                   .isNotEmpty());

        regularExpressionButton.visibleProperty().unbind();
        regularExpressionButton.visibleProperty().bind(focusedOrActive);
        caseSensitiveButton.visibleProperty().unbind();
        caseSensitiveButton.visibleProperty().bind(focusedOrActive);
        fulltextButton.visibleProperty().unbind();
        fulltextButton.visibleProperty().bind(focusedOrActive);

        StackPane modifierButtons = new StackPane(new HBox(regularExpressionButton, caseSensitiveButton, fulltextButton));
        modifierButtons.setAlignment(Pos.CENTER);
        searchField.setRight(new HBox(searchField.getRight(), modifierButtons));
        searchField.getStyleClass().add("search-field");
        searchField.setMinWidth(100);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        regexValidator = new FunctionBasedValidator<>(
                searchField.textProperty(),
                query -> !(regularExpressionButton.isSelected() && !validRegex()),
                ValidationMessage.error(Localization.lang("Invalid regular expression")));
        ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
        visualizer.setDecoration(new IconValidationDecorator(Pos.CENTER_LEFT));
        Platform.runLater(() -> visualizer.initVisualization(regexValidator.getValidationStatus(), searchField));

        this.getChildren().addAll(searchField, currentResults);
        this.setSpacing(4.0);
        this.setAlignment(Pos.CENTER_LEFT);

        Timer searchTask = FxTimer.create(Duration.ofMillis(SEARCH_DELAY), this::updateSearchQuery);
        BindingsHelper.bindBidirectional(
                stateManager.activeGlobalSearchQueryProperty(),
                searchField.textProperty(),
                searchTerm -> {
                    // Async update
                    searchTask.restart();
                },
                query -> setSearchTerm(query.map(SearchQuery::getQuery).orElse("")));

        this.stateManager.activeGlobalSearchQueryProperty().addListener((obs, oldValue, newValue) -> newValue.ifPresent(this::updateSearchResultsForQuery));
        /*
         * The listener tracks a change on the focus property value.
         * This happens, from active (user types a query) to inactive / focus
         * lost (e.g., user selects an entry or triggers the search).
         * The search history should only be filled, if focus is lost.
         */
        searchField.focusedProperty().addListener((obs, oldValue, newValue) -> {
            // Focus lost can be derived by checking that there is no newValue (or the text is empty)
            if (oldValue && !(newValue || searchField.getText().isBlank())) {
                this.stateManager.addSearchHistory(searchField.textProperty().get());
            }
        });
    }

    private void updateSearchResultsForQuery(SearchQuery query) {
        updateResults(this.stateManager.getGlobalSearchResultSize().get(), SearchDescribers.getSearchDescriberFor(query).getDescription(),
                query.isGrammarBasedSearch());
    }

    private void initSearchModifierButtons() {
        regularExpressionButton.setSelected(searchPreferences.isRegularExpression());
        regularExpressionButton.setTooltip(new Tooltip(Localization.lang("regular expression")));
        initSearchModifierButton(regularExpressionButton);
        regularExpressionButton.setOnAction(event -> {
            searchPreferences.setSearchFlag(SearchRules.SearchFlags.REGULAR_EXPRESSION, regularExpressionButton.isSelected());
            updateSearchQuery();
        });

        caseSensitiveButton.setSelected(searchPreferences.isCaseSensitive());
        caseSensitiveButton.setTooltip(new Tooltip(Localization.lang("Case sensitive")));
        initSearchModifierButton(caseSensitiveButton);
        caseSensitiveButton.setOnAction(event -> {
            searchPreferences.setSearchFlag(SearchRules.SearchFlags.CASE_SENSITIVE, caseSensitiveButton.isSelected());
            updateSearchQuery();
        });

        fulltextButton.setSelected(searchPreferences.isFulltext());
        fulltextButton.setTooltip(new Tooltip(Localization.lang("Fulltext search")));
        initSearchModifierButton(fulltextButton);
        fulltextButton.setOnAction(event -> {
            searchPreferences.setSearchFlag(SearchRules.SearchFlags.FULLTEXT, fulltextButton.isSelected());
            updateSearchQuery();
        });
    }

    private void initSearchModifierButton(ButtonBase searchButton) {
        searchButton.setCursor(Cursor.DEFAULT);
        searchButton.setMinHeight(28);
        searchButton.setMaxHeight(28);
        searchButton.setMinWidth(28);
        searchButton.setMaxWidth(28);
        searchButton.setPadding(new Insets(1.0));
        searchButton.managedProperty().bind(searchField.editableProperty());
        searchButton.visibleProperty().bind(searchField.editableProperty());
    }

    /**
     * Focuses the search field if it is not focused.
     */
    public void focus() {
        if (!searchField.isFocused()) {
            searchField.requestFocus();
        }
        searchField.selectAll();
    }

    public void updateSearchQuery() {
        LOGGER.debug("Flags: {}", searchPreferences.getSearchFlags());
        LOGGER.debug("Updated search query: {}", searchField.getText());

        // An empty search field should cause the search to be cleared.
        if (searchField.getText().isEmpty()) {
            currentResults.setText("");
            setSearchFieldHintTooltip(null);
            stateManager.clearGlobalSearchQuery();
            return;
        }

        // Invalid regular expression
        if (!regexValidator.getValidationStatus().isValid()) {
            currentResults.setText(Localization.lang("Invalid regular expression"));
            return;
        }

        SearchQuery searchQuery = new SearchQuery(this.searchField.getText(), searchPreferences.getSearchFlags());
        if (!searchQuery.isValid()) {
            informUserAboutInvalidSearchQuery();
            return;
        }
        stateManager.setGlobalSearchQuery(searchQuery);
    }

    private boolean validRegex() {
        try {
            Pattern.compile(searchField.getText());
        } catch (PatternSyntaxException e) {
            LOGGER.debug(e.getMessage());
            return false;
        }
        return true;
    }

    private void informUserAboutInvalidSearchQuery() {
        searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);

        stateManager.clearGlobalSearchQuery();

        String illegalSearch = Localization.lang("Search failed: illegal search expression");
        currentResults.setText(illegalSearch);
    }

    private void updateResults(int matched, TextFlow description, boolean grammarBasedSearch) {
        if (matched == 0) {
            currentResults.setText(Localization.lang("No results found."));
            searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);
        } else {
            currentResults.setText(Localization.lang("Found %0 results.", String.valueOf(matched)));
            searchField.pseudoClassStateChanged(CLASS_RESULTS_FOUND, true);
        }
        if (grammarBasedSearch) {
            // TODO: switch Icon color
            // searchIcon.setIcon(IconTheme.JabRefIcon.ADVANCED_SEARCH.getIcon());
        } else {
            // TODO: switch Icon color
            // searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getIcon());
        }

        setSearchFieldHintTooltip(description);
    }

    private void setSearchFieldHintTooltip(TextFlow description) {
        if (preferencesService.getWorkspacePreferences().shouldShowAdvancedHints()) {
            String genericDescription = Localization.lang("Hint:\n\nTo search all fields for <b>Smith</b>, enter:\n<tt>smith</tt>\n\nTo search the field <b>author</b> for <b>Smith</b> and the field <b>title</b> for <b>electrical</b>, enter:\n<tt>author=Smith and title=electrical</tt>");
            List<Text> genericDescriptionTexts = TooltipTextUtil.createTextsFromHtml(genericDescription);

            if (description == null) {
                TextFlow emptyHintTooltip = new TextFlow();
                emptyHintTooltip.getChildren().setAll(genericDescriptionTexts);
                searchFieldTooltip.setGraphic(emptyHintTooltip);
            } else {
                description.getChildren().add(new Text("\n\n"));
                description.getChildren().addAll(genericDescriptionTexts);
                searchFieldTooltip.setGraphic(description);
            }
        }
    }

    public void updateHintVisibility() {
        setSearchFieldHintTooltip(null);
    }

    public void setSearchTerm(String searchTerm) {
        if (searchTerm.equals(searchField.getText())) {
            return;
        }

        DefaultTaskExecutor.runInJavaFXThread(() -> searchField.setText(searchTerm));
    }
}
