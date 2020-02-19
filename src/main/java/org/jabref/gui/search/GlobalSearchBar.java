package org.jabref.gui.search;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.AppendPersonNamesStrategy;
import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.autocompleter.PersonNameStringConverter;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.entry.Author;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.SearchPreferences;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import impl.org.controlsfx.skin.AutoCompletePopup;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.fxmisc.easybind.EasyBind;
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

    private final JabRefFrame frame;

    private final TextField searchField = SearchTextField.create();
    private final ToggleButton caseSensitive;
    private final ToggleButton regularExp;
    private final Button searchModeButton = new Button();
    private final Label currentResults = new Label("");
    private final Tooltip tooltip = new Tooltip();
    private final StateManager stateManager;
    private SearchDisplayMode searchDisplayMode;
    private Validator regexValidator;

    public GlobalSearchBar(JabRefFrame frame, StateManager stateManager) {
        super();
        this.frame = Objects.requireNonNull(frame);
        this.stateManager = stateManager;

        SearchPreferences searchPreferences = new SearchPreferences(Globals.prefs);
        searchDisplayMode = searchPreferences.getSearchMode();
        
        this.searchField.disableProperty().bind(needsDatabase(stateManager).not());

        // fits the standard "found x entries"-message thus hinders the searchbar to jump around while searching if the frame width is too small
        currentResults.setPrefWidth(150);

        tooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tooltip.setMaxHeight(10);
        searchField.setTooltip(null);
        updateHintVisibility();

        KeyBindingRepository keyBindingRepository = Globals.getKeyPrefs();
        searchField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                if (keyBinding.get().equals(KeyBinding.CLOSE)) {
                    // Clear search and select first entry, if available
                    searchField.setText("");
                    frame.getCurrentBasePanel().getMainTable().getSelectionModel().selectFirst();
                    event.consume();
                }
            }
        });

        ClipBoardManager.addX11Support(searchField);

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

        regexValidator = new FunctionBasedValidator<>(
                searchField.textProperty(),
                query -> !(regularExp.isSelected() && !validRegex()),
                ValidationMessage.error(Localization.lang("Invalid regular expression"))
        );
        ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
        visualizer.setDecoration(new IconValidationDecorator(Pos.CENTER_LEFT));
        Platform.runLater(() -> { visualizer.initVisualization(regexValidator.getValidationStatus(), searchField); });

        Timer searchTask = FxTimer.create(java.time.Duration.ofMillis(SEARCH_DELAY), this::performSearch);
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

        this.getChildren().addAll(searchField, currentResults);

        this.setAlignment(Pos.CENTER_LEFT);

        BindingsHelper.bindBidirectional(
                stateManager.activeSearchQueryProperty(),
                searchField.textProperty(),
                searchTerm -> {
                    performSearch();
                },
                query -> setSearchTerm(query.map(SearchQuery::getQuery).orElse(""))
        );

        EasyBind.subscribe(this.stateManager.activeSearchQueryProperty(), searchQuery -> {

            searchQuery.ifPresent(query -> {
                updateResults(this.stateManager.getSearchResultSize().intValue(), SearchDescribers.getSearchDescriberFor(query).getDescription(),
                              query.isGrammarBasedSearch());
            });
        });
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
            searchField.setText("");
            MainTable mainTable = frame.getCurrentBasePanel().getMainTable();
            mainTable.requestFocus();
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

    public void performSearch() {
        LOGGER.debug("Run search " + searchField.getText());

        // An empty search field should cause the search to be cleared.
        if (searchField.getText().isEmpty()) {
            currentResults.setText("");
            setHintTooltip(null);
            stateManager.clearSearchQuery();
            return;
        }

        // Invalid regular expression
        if (!regexValidator.getValidationStatus().isValid()) {
            currentResults.setText(Localization.lang("Invalid regular expression"));
            return;
        }

        SearchQuery searchQuery = new SearchQuery(this.searchField.getText(), this.caseSensitive.isSelected(), this.regularExp.isSelected());
        if (!searchQuery.isValid()) {
            informUserAboutInvalidSearchQuery();
            return;
        }
        stateManager.setSearchQuery(searchQuery);
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

        stateManager.clearSearchQuery();

        String illegalSearch = Localization.lang("Search failed: illegal search expression");
        currentResults.setText(illegalSearch);
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
            // TODO: reflective access, should be removed
            Field privatePopup = AutoCompletionBinding.class.getDeclaredField("autoCompletionPopup");
            privatePopup.setAccessible(true);
            return (AutoCompletePopup<T>) privatePopup.get(autoCompletionBinding);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            LOGGER.error("Could not get access to auto completion popup", e);
            return new AutoCompletePopup<>();
        }
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
            //searchIcon.setIcon(IconTheme.JabRefIcon.SEARCH.getIcon());
        }

        setHintTooltip(description);
    }

    private void setHintTooltip(TextFlow description) {
        if (Globals.prefs.getBoolean(JabRefPreferences.SHOW_ADVANCED_HINTS)) {
            String genericDescription = Localization.lang("Hint: To search specific fields only, enter for example:<p><tt>author=smith and title=electrical</tt>");
            genericDescription = genericDescription.replace("<p>", "\n");
            List<Text> genericDescriptionTexts = TooltipTextUtil.formatToTexts(genericDescription, new TooltipTextUtil.TextReplacement("<tt>author=smith and title=electrical</tt>", "author=smith and title=electrical", TooltipTextUtil.TextType.MONOSPACED));

            if (description != null) {
                description.getChildren().add(new Text("\n\n"));
                description.getChildren().addAll(genericDescriptionTexts);
                tooltip.setGraphic(description);
            } else {
                TextFlow emptyHintTooltip = new TextFlow();
                emptyHintTooltip.getChildren().setAll(genericDescriptionTexts);
                tooltip.setGraphic(emptyHintTooltip);
            }
        }
    }

    public void updateHintVisibility() {
        if (Globals.prefs.getBoolean(JabRefPreferences.SHOW_ADVANCED_HINTS)) {
            searchField.setTooltip(tooltip);
        } else {
            searchField.setTooltip(null);
        }
        setHintTooltip(null);
    }

    public void setSearchTerm(String searchTerm) {
        if (searchTerm.equals(searchField.getText())) {
            return;
        }

        DefaultTaskExecutor.runInJavaFXThread(() -> searchField.setText(searchTerm));
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

            ToolBar toolBar = new ToolBar(regularExp, caseSensitive, searchModeButton);

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
            //empty
        }
    }
}
