package org.jabref.gui.search;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
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

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
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
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.TooltipTextUtil;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.entry.Author;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
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

    private final CustomTextField searchField = SearchTextField.create();
    private final ToggleButton caseSensitiveButton;
    private final ToggleButton regularExpressionButton;
    // private final Button searchModeButton;
    private final Label currentResults = new Label("");
    private final Tooltip tooltip = new Tooltip();

    private final StateManager stateManager;
    private final PreferencesService preferencesService;
    private final Validator regexValidator;

    private SearchPreferences searchPreferences;

    public GlobalSearchBar(JabRefFrame frame, StateManager stateManager, PreferencesService preferencesService) {
        super();
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.searchPreferences = preferencesService.getSearchPreferences();

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

        regularExpressionButton = IconTheme.JabRefIcons.REG_EX.asToggleButton();
        caseSensitiveButton = IconTheme.JabRefIcons.CASE_SENSITIVE.asToggleButton();
        // searchModeButton = new Button();
        initSearchModifierButtons();

        BooleanBinding focusBinding = searchField.focusedProperty()
                                                 .or(regularExpressionButton.focusedProperty()
                                                                            .or(caseSensitiveButton.focusedProperty()));
        regularExpressionButton.visibleProperty().unbind();
        regularExpressionButton.visibleProperty().bind(focusBinding);
        caseSensitiveButton.visibleProperty().unbind();
        caseSensitiveButton.visibleProperty().bind(focusBinding);

        StackPane modifierButtons = new StackPane(new HBox(regularExpressionButton, caseSensitiveButton));
        modifierButtons.setAlignment(Pos.CENTER);
        searchField.setRight(new HBox(searchField.getRight(), modifierButtons));
        searchField.getStyleClass().add("search-field");
        searchField.setMinWidth(100);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        regexValidator = new FunctionBasedValidator<>(
                searchField.textProperty(),
                query -> !(regularExpressionButton.isSelected() && !validRegex()),
                ValidationMessage.error(Localization.lang("Invalid regular expression"))
        );
        ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
        visualizer.setDecoration(new IconValidationDecorator(Pos.CENTER_LEFT));
        Platform.runLater(() -> visualizer.initVisualization(regexValidator.getValidationStatus(), searchField));

        this.getChildren().addAll(searchField, currentResults);
        this.setSpacing(4.0);
        this.setAlignment(Pos.CENTER_LEFT);

        Timer searchTask = FxTimer.create(java.time.Duration.ofMillis(SEARCH_DELAY), this::performSearch);
        BindingsHelper.bindBidirectional(
                stateManager.activeSearchQueryProperty(),
                searchField.textProperty(),
                searchTerm -> {
                    // Async update
                    searchTask.restart();
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

    private void initSearchModifierButtons() {
        regularExpressionButton.setSelected(searchPreferences.isRegularExpression());
        regularExpressionButton.setTooltip(new Tooltip(Localization.lang("regular expression")));
        regularExpressionButton.setCursor(Cursor.DEFAULT);
        regularExpressionButton.setMinHeight(28);
        regularExpressionButton.setMaxHeight(28);
        regularExpressionButton.setMinWidth(28);
        regularExpressionButton.setMaxWidth(28);
        regularExpressionButton.setPadding(new Insets(1.0));
        regularExpressionButton.managedProperty().bind(searchField.editableProperty());
        regularExpressionButton.visibleProperty().bind(searchField.editableProperty());
        regularExpressionButton.setOnAction(event -> {
            searchPreferences = searchPreferences.withRegularExpression(regularExpressionButton.isSelected());
            preferencesService.storeSearchPreferences(searchPreferences);
            performSearch();
        });

        caseSensitiveButton.setSelected(searchPreferences.isCaseSensitive());
        caseSensitiveButton.setTooltip(new Tooltip(Localization.lang("Case sensitive")));
        caseSensitiveButton.setCursor(Cursor.DEFAULT);
        caseSensitiveButton.setMinHeight(28);
        caseSensitiveButton.setMaxHeight(28);
        caseSensitiveButton.setMinWidth(28);
        caseSensitiveButton.setMaxWidth(28);
        caseSensitiveButton.setPadding(new Insets(1.0));
        caseSensitiveButton.managedProperty().bind(searchField.editableProperty());
        caseSensitiveButton.visibleProperty().bind(searchField.editableProperty());
        caseSensitiveButton.setOnAction(event -> {
            searchPreferences = searchPreferences.withCaseSensitive(caseSensitiveButton.isSelected());
            preferencesService.storeSearchPreferences(searchPreferences);
            performSearch();
        });

        // ToDo: Reimplement searchMode (searchModeButton)
        /* searchModeButton.setText(searchPreferences.getSearchDisplayMode().getDisplayName());
        searchModeButton.setTooltip(new Tooltip(searchPreferences.getSearchDisplayMode().getToolTipText()));
        searchModeButton.setOnAction(event -> {
            SearchDisplayMode searchDisplayMode = searchPreferences.getSearchDisplayMode();
            int nextSearchMode = (searchDisplayMode.ordinal() + 1) % SearchDisplayMode.values().length;
            searchDisplayMode = SearchDisplayMode.values()[nextSearchMode];

            searchPreferences = searchPreferences..withSearchDisplayMode(searchDisplayMode);
            preferencesService.storeSearchPreferences(searchPreferences);

            searchModeButton.setText(searchDisplayMode.getDisplayName());
            searchModeButton.setTooltip(new Tooltip(searchDisplayMode.getToolTipText()));

            performSearch();
        }); */
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

        SearchQuery searchQuery = new SearchQuery(this.searchField.getText(), searchPreferences.isCaseSensitive(), searchPreferences.isRegularExpression());
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

        setHintTooltip(description);
    }

    private void setHintTooltip(TextFlow description) {
        if (preferencesService.getGeneralPreferences().shouldShowAdvancedHints()) {
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
        if (preferencesService.getGeneralPreferences().shouldShowAdvancedHints()) {
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

            this.container = new BorderPane();
            this.container.setCenter(suggestionList);

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
            // empty
        }
    }
}
