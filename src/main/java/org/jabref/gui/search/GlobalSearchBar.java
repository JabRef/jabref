package org.jabref.gui.search;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.GUIGlobals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.autocompleter.AppendPersonNamesStrategy;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.PersonNameStringConverter;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.search.SearchQueryHighlightObservable;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.SearchPreferences;

import impl.org.controlsfx.skin.AutoCompletePopup;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.fxmisc.easybind.EasyBind;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalSearchBar extends HBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearchBar.class);

    private static final int SEARCH_DELAY = 400;
    private static final PseudoClass CLASS_NO_RESULTS = PseudoClass.getPseudoClass("emptyResult");
    private static final PseudoClass CLASS_RESULTS_FOUND = PseudoClass.getPseudoClass("emptyResult");

    private final JabRefFrame frame;

    private final TextField searchField = SearchTextField.create();
    private final ToggleButton caseSensitive;
    private final ToggleButton regularExp;
    private final ToggleButton globalSearch;
    private final Button searchModeButton = new Button();
    private final Label currentResults = new Label("");
    private final SearchQueryHighlightObservable searchQueryHighlightObservable = new SearchQueryHighlightObservable();
    private final Button openCurrentResultsInDialog = IconTheme.JabRefIcons.OPEN_IN_NEW_WINDOW.asButton();
    private SearchWorker searchWorker;
    private GlobalSearchWorker globalSearchWorker;

    private SearchResultFrame searchResultFrame;

    private SearchDisplayMode searchDisplayMode;

    /**
     * if this flag is set the searchbar won't be selected after the next search
     */
    private boolean dontSelectSearchBar;

    public GlobalSearchBar(JabRefFrame frame) {
        super();
        this.frame = Objects.requireNonNull(frame);
        SearchPreferences searchPreferences = new SearchPreferences(Globals.prefs);
        searchDisplayMode = searchPreferences.getSearchMode();

        // fits the standard "found x entries"-message thus hinders the searchbar to jump around while searching if the frame width is too small
        currentResults.setPrefWidth(150);

        globalSearch = IconTheme.JabRefIcons.GLOBAL_SEARCH.asToggleButton();
        globalSearch.setSelected(searchPreferences.isGlobalSearch());
        globalSearch.setTooltip(new Tooltip(Localization.lang("Search in all open libraries")));


        KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                if (keyBinding.get().equals(KeyBinding.GLOBAL_SEARCH)) {
                    globalSearch.setSelected(true);
                    searchPreferences.setGlobalSearch(globalSearch.isSelected());
                    updateOpenCurrentResultsTooltip(globalSearch.isSelected());
                    focus();
                    event.consume();
                } else if (keyBinding.get().equals(KeyBinding.CLOSE)) {
                    // Clear search and select first entry, if available
                    clearSearch();
                    frame.getCurrentBasePanel().getMainTable().getSelectionModel().selectFirst();
                    event.consume();
                }
            }
        });

        globalSearch.setOnAction(event -> {
            searchPreferences.setGlobalSearch(globalSearch.isSelected());
            updateOpenCurrentResultsTooltip(globalSearch.isSelected());
        });

        //openCurrentResultsInDialog.setDisabledIcon(IconTheme.JabRefIcons.OPEN_IN_NEW_WINDOW.disabled().getSmallIcon());
        openCurrentResultsInDialog.setOnAction(event -> {
            if (globalSearch.isSelected()) {
                performGlobalSearch();
            } else {
                openLocalFindingsInExternalPanel();
            }
        });
        openCurrentResultsInDialog.setDisable(true);
        updateOpenCurrentResultsTooltip(globalSearch.isSelected());

        regularExp = IconTheme.JabRefIcons.REG_EX.asToggleButton();
        regularExp.setSelected(searchPreferences.isRegularExpression());
        regularExp.setTooltip(new Tooltip(Localization.lang("regular expression")));
        regularExp.setOnAction(event -> {
            searchPreferences.setRegularExpression(regularExp.isSelected());
            performSearch();
        });

        caseSensitive = IconTheme.JabRefIcons.CASE_SENSITIVE.asToggleButton();
        caseSensitive.setSelected(searchPreferences.isCaseSensitive());
        caseSensitive.setTooltip(new Tooltip(Localization.lang("Case sensitive")));
        caseSensitive.setOnAction(event -> {
            searchPreferences.setCaseSensitive(caseSensitive.isSelected());
            performSearch();
        });

        updateSearchModeButtonText();
        searchModeButton.setOnAction(event -> toggleSearchModeAndSearch());

        int initialSize = 400;
        int expandedSize = 700;
        searchField.getStyleClass().add("search-field");
        searchField.setMinWidth(100);
        searchField.setMaxWidth(initialSize);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Timer searchTask = FxTimer.create(java.time.Duration.ofMillis(SEARCH_DELAY), () -> {
            LOGGER.debug("Run search " + searchField.getText());
            performSearch();
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) -> searchTask.restart());

        EasyBind.subscribe(searchField.focusedProperty(), isFocused -> {
            if (isFocused) {
                KeyValue widthValue = new KeyValue(searchField.maxWidthProperty(), expandedSize);
                KeyFrame keyFrame = new KeyFrame(Duration.millis(600), widthValue);
                Timeline timeline = new Timeline(keyFrame);
                timeline.play();
            } else {
                KeyValue widthValue = new KeyValue(searchField.maxWidthProperty(), initialSize);
                KeyFrame keyFrame = new KeyFrame(Duration.millis(400), widthValue);
                Timeline timeline = new Timeline(keyFrame);
                timeline.play();
            }
        });

        this.getChildren().addAll(
                                  searchField,
                                  currentResults);

        this.setAlignment(Pos.CENTER_LEFT);
    }

    private void performGlobalSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if ((currentBasePanel == null) || validateSearchResultFrame(true)) {
            return;
        }

        if (globalSearchWorker != null) {
            globalSearchWorker.cancel(true);
        }

        if (searchField.getText().isEmpty()) {
            focus();
            return;
        }

        globalSearchWorker = new GlobalSearchWorker(currentBasePanel.frame(), getSearchQuery());
        globalSearchWorker.execute();
    }

    private void openLocalFindingsInExternalPanel() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if ((currentBasePanel == null) || validateSearchResultFrame(false)) {
            return;
        }

        if (searchField.getText().isEmpty()) {
            focus();
            return;
        }

        SearchResultFrame searchDialog = new SearchResultFrame(currentBasePanel.frame(), Localization.lang("Search results in library %0 for %1", currentBasePanel.getBibDatabaseContext()
                                                                                                                                                                  .getDatabasePath()
                                                                                                                                                                  .map(Path::getFileName)
                                                                                                                                                                  .map(Path::toString)
                                                                                                                                                                  .orElse(GUIGlobals.UNTITLED_TITLE),
                                                                                                           this.getSearchQuery().localize()), getSearchQuery(), false);
        List<BibEntry> entries = currentBasePanel.getDatabase()
                                                 .getEntries()
                                                 .stream()
                                                 .filter(BibEntry::isSearchHit)
                                                 .collect(Collectors.toList());
        searchDialog.addEntries(entries, currentBasePanel);
        searchDialog.selectFirstEntry();
        searchDialog.setVisible(true);
    }

    private boolean validateSearchResultFrame(boolean globalSearch) {
        if (searchResultFrame != null) {
            if ((searchResultFrame.isGlobalSearch() == globalSearch) && isStillValidQuery(searchResultFrame.getSearchQuery())) {
                searchResultFrame.focus();
                return true;
            } else {
                searchResultFrame.dispose();
                return false;
            }
        }

        return false;
    }

    private void toggleSearchModeAndSearch() {
        int nextSearchMode = (searchDisplayMode.ordinal() + 1) % SearchDisplayMode.values().length;
        searchDisplayMode = SearchDisplayMode.values()[nextSearchMode];
        new SearchPreferences(Globals.prefs).setSearchMode(searchDisplayMode);
        updateSearchModeButtonText();
        performSearch();
    }

    private void updateSearchModeButtonText() {
        searchModeButton.setText(searchDisplayMode.getDisplayName());
        searchModeButton.setTooltip(new Tooltip(searchDisplayMode.getToolTipText()));
    }

    public void endSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel != null) {
            clearSearch();
            MainTable mainTable = frame.getCurrentBasePanel().getMainTable();
            //Globals.getFocusListener().setFocused(mainTable);
            mainTable.requestFocus();
            //SwingUtilities.invokeLater(() -> mainTable.ensureVisible(mainTable.getSelectedRow()));
        }
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

    private void clearSearch() {
        currentResults.setText("");
        searchField.setText("");
        searchQueryHighlightObservable.reset();
        openCurrentResultsInDialog.setDisable(true);

        Globals.stateManager.clearSearchQuery();
    }

    public void performSearch() {
        BasePanel currentBasePanel = frame.getCurrentBasePanel();
        if (currentBasePanel == null) {
            return;
        }

        if (searchWorker != null) {
            searchWorker.cancel(true);
        }

        // An empty search field should cause the search to be cleared.
        if (searchField.getText().isEmpty()) {
            clearSearch();
            return;
        }

        SearchQuery searchQuery = getSearchQuery();
        if (!searchQuery.isValid()) {
            informUserAboutInvalidSearchQuery();
            return;
        }

        Globals.stateManager.setSearchQuery(searchQuery);

        // TODO: Remove search worker as this is doing the work twice now
        searchWorker = new SearchWorker(currentBasePanel, searchQuery, searchDisplayMode);
        searchWorker.execute();
    }

    private void informUserAboutInvalidSearchQuery() {
        searchField.pseudoClassStateChanged(CLASS_NO_RESULTS, true);

        searchQueryHighlightObservable.reset();

        Globals.stateManager.clearSearchQuery();

        String illegalSearch = Localization.lang("Search failed: illegal search expression");
        currentResults.setText(illegalSearch);
        openCurrentResultsInDialog.setDisable(true);
    }

    public void setAutoCompleter(AutoCompleteSuggestionProvider<Author> searchCompleter) {
        if (Globals.prefs.getAutoCompletePreferences().shouldAutoComplete()) {
            AutoCompletionTextInputBinding<Author> autoComplete = AutoCompletionTextInputBinding.autoComplete(searchField,
                                                                                                              searchCompleter,
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
            Field privatePopup = AutoCompletionBinding.class.getDeclaredField("autoCompletionPopup");
            privatePopup.setAccessible(true);
            return (AutoCompletePopup<T>) privatePopup.get(autoCompletionBinding);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            LOGGER.error("Could not get access to auto completion popup", e);
            return new AutoCompletePopup<>();
        }
    }

    public SearchQueryHighlightObservable getSearchQueryHighlightObservable() {
        return searchQueryHighlightObservable;
    }

    public boolean isStillValidQuery(SearchQuery query) {
        return query.getQuery().equals(this.searchField.getText())
                && (query.isRegularExpression() == regularExp.isSelected())
                && (query.isCaseSensitive() == caseSensitive.isSelected());
    }

    private SearchQuery getSearchQuery() {
        SearchQuery searchQuery = new SearchQuery(this.searchField.getText(), this.caseSensitive.isSelected(), this.regularExp.isSelected());
        this.frame.getCurrentBasePanel().setCurrentSearchQuery(searchQuery);
        return searchQuery;
    }

    public void updateResults(int matched, TextFlow description, boolean grammarBasedSearch) {
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
            //searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getIcon());
        }
        Tooltip tooltip = new Tooltip();
        tooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tooltip.setGraphic(description);
        tooltip.setMaxHeight(10);
        searchField.setTooltip(tooltip);
        openCurrentResultsInDialog.setDisable(false);
    }

    public void setSearchResultFrame(SearchResultFrame searchResultFrame) {
        this.searchResultFrame = searchResultFrame;
    }

    public void setSearchTerm(String searchTerm) {
        if (searchTerm.equals(searchField.getText())) {
            return;
        }

        DefaultTaskExecutor.runInJavaFXThread(() -> searchField.setText(searchTerm));
    }

    private void updateOpenCurrentResultsTooltip(boolean globalSearchEnabled) {
        if (globalSearchEnabled) {
            openCurrentResultsInDialog.setTooltip(new Tooltip(Localization.lang("Show global search results in a window")));
        } else {
            openCurrentResultsInDialog.setTooltip(new Tooltip(Localization.lang("Show search results in a window")));
        }
    }

    private class SearchPopupSkin<T> implements Skin<AutoCompletePopup<T>> {

        private final AutoCompletePopup<T> control;
        private final ListView<T> suggestionList;
        private final BorderPane container;

        public SearchPopupSkin(AutoCompletePopup<T> control) {
            this.control = control;
            this.suggestionList = new ListView<>(control.getSuggestions());
            this.suggestionList.getStyleClass().add("auto-complete-popup");
            this.suggestionList.getStylesheets().add(AutoCompletionBinding.class.getResource("autocompletion.css").toExternalForm());
            this.suggestionList.prefHeightProperty().bind(Bindings.min(control.visibleRowCountProperty(), Bindings.size(this.suggestionList.getItems())).multiply(24).add(18));
            this.suggestionList.setCellFactory(TextFieldListCell.forListView(control.getConverter()));
            this.suggestionList.prefWidthProperty().bind(control.prefWidthProperty());
            this.suggestionList.maxWidthProperty().bind(control.maxWidthProperty());
            this.suggestionList.minWidthProperty().bind(control.minWidthProperty());

            ToolBar toolBar = new ToolBar(openCurrentResultsInDialog, new Separator(Orientation.VERTICAL), globalSearch, regularExp, caseSensitive, searchModeButton);

            this.container = new BorderPane();
            this.container.setCenter(suggestionList);
            this.container.setBottom(toolBar);

            this.registerEventListener();
        }

        private void registerEventListener() {
            this.suggestionList.setOnMouseClicked((me) -> {
                if (me.getButton() == MouseButton.PRIMARY) {
                    this.onSuggestionChosen(this.suggestionList.getSelectionModel().getSelectedItem());
                }
            });
            this.suggestionList.setOnKeyPressed((ke) -> {
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
        }
    }
}
