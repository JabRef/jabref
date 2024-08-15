package org.jabref.gui.search;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.AppendPersonNamesStrategy;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.PersonNameStringConverter;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.entry.Author;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.tobiasdiez.easybind.EasyBind;
import impl.org.controlsfx.skin.AutoCompletePopup;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.CustomTextField;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class GlobalSearchBar extends HBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchBar.class);

    private static final int SEARCH_DELAY = 400;
    private static final PseudoClass CLASS_NO_RESULTS = PseudoClass.getPseudoClass("emptyResult");
    private static final PseudoClass CLASS_RESULTS_FOUND = PseudoClass.getPseudoClass("emptyResult");

    private final CustomTextField searchField;
    private final ToggleButton caseSensitiveButton;
    private final ToggleButton regularExpressionButton;
    private final ToggleButton fulltextButton;
    private final Button openGlobalSearchButton;
    private final ToggleButton keepSearchString;
    private final ToggleButton filterModeButton;
    private final Tooltip searchFieldTooltip = new Tooltip();
    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final UndoManager undoManager;
    private final LibraryTabContainer tabContainer;
    private final SearchPreferences searchPreferences;
    private final DialogService dialogService;
    private final BooleanProperty globalSearchActive = new SimpleBooleanProperty(false);
    private final BooleanProperty illegalSearch = new SimpleBooleanProperty(false);
    private final BooleanProperty invalidRegex = new SimpleBooleanProperty(false);
    private GlobalSearchResultDialog globalSearchResultDialog;
    private final SearchType searchType;

    public GlobalSearchBar(LibraryTabContainer tabContainer,
                           StateManager stateManager,
                           PreferencesService preferencesService,
                           UndoManager undoManager,
                           DialogService dialogService,
                           SearchType searchType) {
        super();
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.searchPreferences = preferencesService.getSearchPreferences();
        this.undoManager = undoManager;
        this.dialogService = dialogService;
        this.tabContainer = tabContainer;
        this.searchType = searchType;

        KeyBindingRepository keyBindingRepository = preferencesService.getKeyBindingRepository();

        searchField = SearchTextField.create(keyBindingRepository);
        searchField.disableProperty().bind(needsDatabase(stateManager).not());

        Label currentResults = new Label();
        // fits the standard "found x entries"-message thus hinders the searchbar to jump around while searching if the tabContainer width is too small
        currentResults.setPrefWidth(150);
        currentResults.visibleProperty().bind(stateManager.activeSearchQuery(searchType).isPresent());

        currentResults.textProperty().bind(EasyBind.combine(
                stateManager.searchResultSize(searchType), illegalSearch, invalidRegex,
                (matched, illegal, invalid) -> {
                    if (illegal) {
                        searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);
                        return Localization.lang("Search failed: illegal search expression");
                    } else if (invalid) {
                        searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);
                        return Localization.lang("Invalid regular expression");
                    } else if (matched.intValue() == 0) {
                        searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);
                        return Localization.lang("No results found.");
                    } else {
                        searchField.pseudoClassStateChanged(CLASS_RESULTS_FOUND, true);
                        return Localization.lang("Found %0 results.", String.valueOf(matched));
                    }
                }
        ));

        searchField.setTooltip(searchFieldTooltip);
        searchFieldTooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        searchFieldTooltip.setMaxHeight(10);
        updateHintVisibility();

        searchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (keyBindingRepository.matches(event, KeyBinding.CLEAR_SEARCH)) {
                searchField.clear();
                if (searchType == SearchType.NORMAL_SEARCH) {
                    tabContainer.getCurrentLibraryTab().getMainTable().requestFocus();
                }
                event.consume();
            }
        });

        ClipBoardManager.addX11Support(searchField);

        searchField.setContextMenu(SearchFieldRightClickMenu.create(stateManager, searchField));
        stateManager.getWholeSearchHistory().addListener((ListChangeListener.Change<? extends String> change) -> {
            searchField.getContextMenu().getItems().removeLast();
            searchField.getContextMenu().getItems().add(SearchFieldRightClickMenu.createSearchFromHistorySubMenu(stateManager, searchField));
        });

        regularExpressionButton = IconTheme.JabRefIcons.REG_EX.asToggleButton();
        caseSensitiveButton = IconTheme.JabRefIcons.CASE_SENSITIVE.asToggleButton();
        fulltextButton = IconTheme.JabRefIcons.FULLTEXT.asToggleButton();
        openGlobalSearchButton = IconTheme.JabRefIcons.OPEN_GLOBAL_SEARCH.asButton();
        keepSearchString = IconTheme.JabRefIcons.KEEP_SEARCH_STRING.asToggleButton();
        filterModeButton = IconTheme.JabRefIcons.FILTER.asToggleButton();

        initSearchModifierButtons();

        BooleanBinding focusedOrActive = searchField.focusedProperty()
                                                    .or(regularExpressionButton.focusedProperty())
                                                    .or(caseSensitiveButton.focusedProperty())
                                                    .or(fulltextButton.focusedProperty())
                                                    .or(keepSearchString.focusedProperty())
                                                    .or(filterModeButton.focusedProperty())
                                                    .or(searchField.textProperty().isNotEmpty());

        regularExpressionButton.visibleProperty().unbind();
        regularExpressionButton.visibleProperty().bind(focusedOrActive);
        caseSensitiveButton.visibleProperty().unbind();
        caseSensitiveButton.visibleProperty().bind(focusedOrActive);
        fulltextButton.visibleProperty().unbind();
        fulltextButton.visibleProperty().bind(focusedOrActive);
        keepSearchString.visibleProperty().unbind();
        keepSearchString.visibleProperty().bind(focusedOrActive);
        filterModeButton.visibleProperty().unbind();
        filterModeButton.visibleProperty().bind(focusedOrActive);

        StackPane modifierButtons;
        if (searchType == SearchType.NORMAL_SEARCH) {
            modifierButtons = new StackPane(new HBox(regularExpressionButton, caseSensitiveButton, fulltextButton, keepSearchString, filterModeButton));
        } else {
            modifierButtons = new StackPane(new HBox(regularExpressionButton, caseSensitiveButton, fulltextButton));
        }

        modifierButtons.setAlignment(Pos.CENTER);
        searchField.setRight(new HBox(searchField.getRight(), modifierButtons));
        searchField.getStyleClass().add("global-search-bar");
        searchField.setMinWidth(100);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        if (searchType == SearchType.NORMAL_SEARCH) {
            this.getChildren().addAll(searchField, openGlobalSearchButton, currentResults);
        } else {
            this.getChildren().addAll(searchField, currentResults);
        }

        this.setSpacing(4.0);
        this.setAlignment(Pos.CENTER_LEFT);

        Timer searchTask = FxTimer.create(Duration.ofMillis(SEARCH_DELAY), this::updateSearchQuery);
        BindingsHelper.bindBidirectional(
                stateManager.activeSearchQuery(searchType),
                searchField.textProperty(),
                searchTerm -> {
                    // Async update
                    searchTask.restart();
                },
                query -> setSearchTerm(query.map(SearchQuery::getQuery).orElse("")));

        stateManager.activeSearchQuery(searchType).addListener((obs, oldValue, newValue) ->
                newValue.ifPresent(query -> setSearchFieldHintTooltip(SearchDescribers.getSearchDescriberFor(query).getDescription())));
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

    private void initSearchModifierButtons() {
        regularExpressionButton.setSelected(searchPreferences.isRegularExpression());
        regularExpressionButton.setTooltip(new Tooltip(Localization.lang("regular expression")));
        initSearchModifierButton(regularExpressionButton);
        regularExpressionButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            searchPreferences.setSearchFlag(SearchRules.SearchFlags.REGULAR_EXPRESSION, newVal);
            updateSearchQuery();
        });

        caseSensitiveButton.setSelected(searchPreferences.isCaseSensitive());
        caseSensitiveButton.setTooltip(new Tooltip(Localization.lang("Case sensitive")));
        initSearchModifierButton(caseSensitiveButton);
        caseSensitiveButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            searchPreferences.setSearchFlag(SearchRules.SearchFlags.CASE_SENSITIVE, newVal);
            updateSearchQuery();
        });

        fulltextButton.setSelected(searchPreferences.isFulltext());
        fulltextButton.setTooltip(new Tooltip(Localization.lang("Fulltext search")));
        initSearchModifierButton(fulltextButton);
        fulltextButton.selectedProperty().addListener((obs, oldVal, newVal) -> {
            searchPreferences.setSearchFlag(SearchRules.SearchFlags.FULLTEXT, newVal);
            updateSearchQuery();
        });

        keepSearchString.setSelected(searchPreferences.shouldKeepSearchString());
        keepSearchString.setTooltip(new Tooltip(Localization.lang("Keep search string across libraries")));
        initSearchModifierButton(keepSearchString);
        keepSearchString.selectedProperty().addListener((obs, oldVal, newVal) -> searchPreferences.setKeepSearchString(newVal));

        filterModeButton.setSelected(searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FILTER);
        filterModeButton.setTooltip(new Tooltip(Localization.lang("Filter search results")));
        initSearchModifierButton(filterModeButton);
        filterModeButton.setOnAction(event -> searchPreferences.setSearchDisplayMode(filterModeButton.isSelected() ? SearchDisplayMode.FILTER : SearchDisplayMode.FLOAT));

        openGlobalSearchButton.disableProperty().bindBidirectional(globalSearchActive);
        openGlobalSearchButton.setTooltip(new Tooltip(Localization.lang("Search across libraries in a new window")));
        initSearchModifierButton(openGlobalSearchButton);
        openGlobalSearchButton.setOnAction(evt -> openGlobalSearchDialog());

        searchPreferences.getObservableSearchFlags().addListener((SetChangeListener.Change<? extends SearchRules.SearchFlags> change) -> {
            regularExpressionButton.setSelected(searchPreferences.isRegularExpression());
            caseSensitiveButton.setSelected(searchPreferences.isCaseSensitive());
            fulltextButton.setSelected(searchPreferences.isFulltext());
        });
    }

    public void openGlobalSearchDialog() {
        if (globalSearchActive.get()) {
            return;
        }
        globalSearchActive.setValue(true);
        if (globalSearchResultDialog == null) {
            globalSearchResultDialog = new GlobalSearchResultDialog(undoManager, tabContainer);
        }
        stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH).get().ifPresent(query ->
                    stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH).set(Optional.of(query)));
        updateSearchQuery();
        dialogService.showCustomDialogAndWait(globalSearchResultDialog);
        globalSearchActive.setValue(false);
    }

    private void initSearchModifierButton(ButtonBase searchButton) {
        searchButton.setCursor(Cursor.HAND);
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
    @Override
    public void requestFocus() {
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
            setSearchFieldHintTooltip(null);
            stateManager.activeSearchQuery(searchType).set(Optional.empty());
            return;
        }

        // Invalid regular expression
        if (regularExpressionButton.isSelected() && !validRegex()) {
            invalidRegex.setValue(true);
            return;
        } else {
            invalidRegex.setValue(false);
        }

        SearchQuery searchQuery = new SearchQuery(this.searchField.getText(), searchPreferences.getSearchFlags());
        if (!searchQuery.isValid()) {
            illegalSearch.set(true);
            return;
        } else {
            illegalSearch.set(false);
        }
        stateManager.activeSearchQuery(searchType).set(Optional.of(searchQuery));
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

    public void setAutoCompleter(SuggestionProvider<Author> searchCompleter) {
        if (preferencesService.getAutoCompletePreferences().shouldAutoComplete()) {
            AutoCompletionTextInputBinding<Author> autoComplete = AutoCompletionTextInputBinding.autoComplete(searchField,
                    searchCompleter::provideSuggestions,
                    new PersonNameStringConverter(false, false, AutoCompleteFirstNameMode.BOTH),
                    new AppendPersonNamesStrategy());
            AutoCompletePopup<Author> popup = getPopup(autoComplete);
            popup.setSkin(new SearchPopupSkin<>(popup));
        }
    }

    /**
     * The popup has private access in {@link AutoCompletionBinding}, so we use reflection to access it.
     */
    @SuppressWarnings("unchecked")
    private <T> AutoCompletePopup<T> getPopup(AutoCompletionBinding<T> autoCompletionBinding) {
        try {
            // TODO: reflective access, should be removed
            Field privatePopup = AutoCompletionBinding.class.getDeclaredField("autoCompletionPopup");
            privatePopup.setAccessible(true);
            return (AutoCompletePopup<T>) privatePopup.get(autoCompletionBinding);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            LOGGER.error("Could not get access to auto completion popup", e);
            return new AutoCompletePopup<>();
        }
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

        UiTaskExecutor.runInJavaFXThread(() -> searchField.setText(searchTerm));
    }

    @AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
    private static class SearchPopupSkin<T> implements Skin<AutoCompletePopup<T>> {

        private final AutoCompletePopup<T> control;
        private final ListView<T> suggestionList;
        private final BorderPane container;

        public SearchPopupSkin(AutoCompletePopup<T> control) {
            this.control = control;
            this.suggestionList = new ListView<>(control.getSuggestions());
            this.suggestionList.getStyleClass().add("auto-complete-popup");
            this.suggestionList.getStylesheets().add(Objects.requireNonNull(AutoCompletionBinding.class.getResource("autocompletion.css")).toExternalForm());
            this.suggestionList.prefHeightProperty().bind(Bindings.min(control.visibleRowCountProperty(), Bindings.size(this.suggestionList.getItems())).multiply(24).add(18));
            this.suggestionList.setCellFactory(TextFieldListCell.forListView(control.getConverter()));
            this.suggestionList.prefWidthProperty().bind(control.prefWidthProperty());
            this.suggestionList.maxWidthProperty().bind(control.maxWidthProperty());
            this.suggestionList.minWidthProperty().bind(control.minWidthProperty());

            this.container = new BorderPane();
            this.container.setCenter(suggestionList);

            this.registerEventListener();
        }

        private void registerEventListener() {
            this.suggestionList.setOnMouseClicked(me -> {
                if (me.getButton() == MouseButton.PRIMARY) {
                    this.onSuggestionChosen(this.suggestionList.getSelectionModel().getSelectedItem());
                }
            });
            this.suggestionList.setOnKeyPressed(ke -> {
                switch (ke.getCode()) {
                    case TAB:
                    case ENTER:
                        this.onSuggestionChosen(this.suggestionList.getSelectionModel().getSelectedItem());
                        break;
                    case ESCAPE:
                        if (this.control.isHideOnEscape()) {
                            this.control.hide();
                        }
                        break;
                    default:
                        break;
                }
            });
        }

        private void onSuggestionChosen(T suggestion) {
            if (suggestion != null) {
                Event.fireEvent(this.control, new AutoCompletePopup.SuggestionEvent<>(suggestion));
            }
        }

        @Override
        public Node getNode() {
            return this.container;
        }

        @Override
        public AutoCompletePopup<T> getSkinnable() {
            return this.control;
        }

        @Override
        public void dispose() {
            // empty
        }
    }
}
