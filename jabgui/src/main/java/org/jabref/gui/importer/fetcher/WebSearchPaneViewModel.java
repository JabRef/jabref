package org.jabref.gui.importer.fetcher;

import java.util.Optional;
import java.util.concurrent.Callable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.frame.SidePanePreferences;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSearchPaneViewModel {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSearchPaneViewModel.class);

    private final ObjectProperty<SearchBasedFetcher> selectedFetcher = new SimpleObjectProperty<>();
    private final ListProperty<SearchBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty query = new SimpleStringProperty();
    private final BooleanProperty identifierDetected = new SimpleBooleanProperty(false);
    private final StringProperty detectedIdentifierType = new SimpleStringProperty("");
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final StateManager stateManager;

    private final Validator searchQueryValidator;

    public WebSearchPaneViewModel(GuiPreferences preferences, DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.preferences = preferences;

        fetchers.setAll(WebFetchers.getSearchBasedFetchers(
                preferences.getImportFormatPreferences(),
                preferences.getImporterPreferences()));

        // Choose last-selected fetcher as default
        SidePanePreferences sidePanePreferences = preferences.getSidePanePreferences();
        int defaultFetcherIndex = sidePanePreferences.getWebSearchFetcherSelected();
        if ((defaultFetcherIndex <= 0) || (defaultFetcherIndex >= fetchers.size())) {
            selectedFetcherProperty().setValue(fetchers.getFirst());
        } else {
            selectedFetcherProperty().setValue(fetchers.get(defaultFetcherIndex));
        }
        EasyBind.subscribe(selectedFetcherProperty(), newFetcher -> {
            int newIndex = fetchers.indexOf(newFetcher);
            sidePanePreferences.setWebSearchFetcherSelected(newIndex);
        });

        // Subscribe to query changes to detect identifiers
        EasyBind.subscribe(query, this::updateIdentifierDetection);

        searchQueryValidator = new FunctionBasedValidator<>(
                query,
                queryText -> {
                    if (StringUtil.isBlank(queryText)) {
                        // in case user did not enter something, it is treated as valid (to avoid UI WTFs)
                        return null;
                    }

                    if (CompositeIdFetcher.containsValidId(queryText)) {
                        // in case the query contains any ID, it is treated as valid
                        return null;
                    }

                    try {
                        // The result is ignored because we just check for validity
                        SearchQuery.getStartContext(queryText);
                        return null;
                    } catch (ParseCancellationException e) {
                        // RecognitionException can point out the exact error
                        if (e.getCause() instanceof RecognitionException recEx) {
                            Token offendingToken = recEx.getOffendingToken();

                            // The character position is 0-based, so we add 1 for user-friendliness.
                            int line = offendingToken.getLine();
                            int charPositionInLine = offendingToken.getCharPositionInLine() + 1;

                            return ValidationMessage.error(Localization.lang("Invalid query element '%0' at position %1", offendingToken.getText(), charPositionInLine));
                        }

                        // Fallback for other failing reasons
                        return ValidationMessage.error(Localization.lang("Invalid query"));
                    }
                });
    }

    public ObservableList<SearchBasedFetcher> getFetchers() {
        return fetchers.get();
    }

    public ListProperty<SearchBasedFetcher> fetchersProperty() {
        return fetchers;
    }

    public SearchBasedFetcher getSelectedFetcher() {
        return selectedFetcher.get();
    }

    public ObjectProperty<SearchBasedFetcher> selectedFetcherProperty() {
        return selectedFetcher;
    }

    public String getQuery() {
        return query.get();
    }

    public StringProperty queryProperty() {
        return query;
    }

    public void search() {
        String query = getQuery().trim();
        LOGGER.debug("Starting web search with query: '{}'", query);
        
        if (!preferences.getImporterPreferences().areImporterEnabled()) {
            LOGGER.warn("Web search attempted but importers are disabled");
            dialogService.notify(Localization.lang("Web search disabled"));
            return;
        }

        if (StringUtil.isBlank(query)) {
            LOGGER.warn("Web search attempted with empty query");
            dialogService.notify(Localization.lang("Please enter a search string"));
            return;
        }
        if (stateManager.getActiveDatabase().isEmpty()) {
            LOGGER.warn("Web search attempted but no database is open");
            dialogService.notify(Localization.lang("Please open or start a new library before searching"));
            return;
        }

        SearchBasedFetcher activeFetcher = getSelectedFetcher();
        LOGGER.info("Performing web search using fetcher: {} with query: '{}'", 
                activeFetcher.getName(), query);

        Callable<ParserResult> parserResultCallable;

        String fetcherName = activeFetcher.getName();

        if (CompositeIdFetcher.containsValidId(query)) {
            LOGGER.debug("Query contains valid identifier, using CompositeIdFetcher");
            CompositeIdFetcher compositeIdFetcher = new CompositeIdFetcher(preferences.getImportFormatPreferences());
            parserResultCallable = () -> new ParserResult(OptionalUtil.toList(compositeIdFetcher.performSearchById(query)));
            fetcherName = Localization.lang("Identifier-based Web Search");
        } else {
            LOGGER.debug("Query is a regular search query, using selected fetcher");
            // Exceptions are handled below at "task.onFailure(dialogService::showErrorDialogAndWait)"
            parserResultCallable = () -> new ParserResult(activeFetcher.performSearch(query));
        }

        BackgroundTask<ParserResult> task = BackgroundTask.wrap(parserResultCallable)
                                                          .withInitialMessage(Localization.lang("Processing \"%0\"...", query));
        task.onFailure(exception -> {
            LOGGER.error("Web search failed for query '{}' using fetcher '{}': {}", 
                    query, activeFetcher.getName(), exception.getMessage());
            dialogService.showErrorDialogAndWait(exception);
        });

        ImportEntriesDialog dialog = new ImportEntriesDialog(stateManager.getActiveDatabase().get(), task, activeFetcher, query);
        dialog.setTitle(fetcherName);
        dialogService.showCustomDialogAndWait(dialog);
        
        LOGGER.debug("Web search dialog completed for query: '{}'", query);
    }

    public ValidationStatus queryValidationStatus() {
        return searchQueryValidator.getValidationStatus();
    }

    public boolean isIdentifierDetected() {
        return identifierDetected.get();
    }

    public BooleanProperty identifierDetectedProperty() {
        return identifierDetected;
    }

    public String getDetectedIdentifierType() {
        return detectedIdentifierType.get();
    }

    public StringProperty detectedIdentifierTypeProperty() {
        return detectedIdentifierType;
    }

    private void updateIdentifierDetection(String queryText) {
        if (StringUtil.isBlank(queryText)) {
            identifierDetected.set(false);
            detectedIdentifierType.set("");
            LOGGER.debug("Identifier detection cleared for empty query");
            return;
        }

        Optional<Identifier> identifier = Identifier.from(queryText.trim());
        if (identifier.isPresent()) {
            String identifierType = getIdentifierTypeName(identifier.get());
            identifierDetected.set(true);
            detectedIdentifierType.set(identifierType);
            LOGGER.debug("Identifier detected: {} for query: '{}'", identifierType, queryText);
        } else {
            identifierDetected.set(false);
            detectedIdentifierType.set("");
            LOGGER.debug("No identifier detected for query: '{}'", queryText);
        }
    }

    private String getIdentifierTypeName(Identifier identifier) {
        String className = identifier.getClass().getSimpleName();
        switch (className) {
            case "DOI":
                return Localization.lang("DOI");
            case "ArXivIdentifier":
                return Localization.lang("ArXiv");
            case "ISBN":
                return Localization.lang("ISBN");
            case "SSRN":
                return Localization.lang("SSRN");
            case "RFC":
                return Localization.lang("RFC");
            default:
                return className;
        }
    }
}