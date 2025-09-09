package org.jabref.gui.importer.fetcher;

import java.util.concurrent.Callable;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
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

public class WebSearchPaneViewModel {

    private final ObjectProperty<SearchBasedFetcher> selectedFetcher = new SimpleObjectProperty<>();
    private final ListProperty<SearchBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty query = new SimpleStringProperty();
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

                            return ValidationMessage.error(Localization.lang("Invalid query element '%0' at position %1", line, charPositionInLine));
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
        if (!preferences.getImporterPreferences().areImporterEnabled()) {
            if (!preferences.getImporterPreferences().areImporterEnabled()) {
                dialogService.notify(Localization.lang("Web search disabled"));
                return;
            }
        }

        String query = getQuery().trim();
        if (StringUtil.isBlank(query)) {
            dialogService.notify(Localization.lang("Please enter a search string"));
            return;
        }
        if (stateManager.getActiveDatabase().isEmpty()) {
            dialogService.notify(Localization.lang("Please open or start a new library before searching"));
            return;
        }

        SearchBasedFetcher activeFetcher = getSelectedFetcher();

        Callable<ParserResult> parserResultCallable;

        String fetcherName = activeFetcher.getName();

        if (CompositeIdFetcher.containsValidId(query)) {
            CompositeIdFetcher compositeIdFetcher = new CompositeIdFetcher(preferences.getImportFormatPreferences());
            parserResultCallable = () -> new ParserResult(OptionalUtil.toList(compositeIdFetcher.performSearchById(query)));
            fetcherName = Localization.lang("Identifier-based Web Search");
        } else {
            // Exceptions are handled below at "task.onFailure(dialogService::showErrorDialogAndWait)"
            parserResultCallable = () -> new ParserResult(activeFetcher.performSearch(query));
        }

        BackgroundTask<ParserResult> task = BackgroundTask.wrap(parserResultCallable)
                                                          .withInitialMessage(Localization.lang("Processing \"%0\"...", query));
        task.onFailure(dialogService::showErrorDialogAndWait);

        ImportEntriesDialog dialog = new ImportEntriesDialog(stateManager.getActiveDatabase().get(), task, activeFetcher, query);
        dialog.setTitle(fetcherName);
        dialogService.showCustomDialogAndWait(dialog);
    }

    public ValidationStatus queryValidationStatus() {
        return searchQueryValidator.getValidationStatus();
    }
}
