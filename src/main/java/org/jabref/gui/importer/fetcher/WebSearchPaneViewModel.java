package org.jabref.gui.importer.fetcher;

import java.util.Map;
import java.util.SortedSet;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SidePanePreferences;

import com.tobiasdiez.easybind.EasyBind;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.apache.lucene.queryparser.flexible.core.QueryNodeParseException;
import org.apache.lucene.queryparser.flexible.core.parser.SyntaxParser;
import org.apache.lucene.queryparser.flexible.standard.parser.ParseException;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;

import static org.jabref.logic.importer.fetcher.transformers.AbstractQueryTransformer.NO_EXPLICIT_FIELD;

public class WebSearchPaneViewModel {

    private final ObjectProperty<SearchBasedFetcher> selectedFetcher = new SimpleObjectProperty<>();
    private final ListProperty<SearchBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty query = new SimpleStringProperty();
    private final DialogService dialogService;
    private final StateManager stateManager;

    private final Validator searchQueryValidator;
    private final SyntaxParser parser = new StandardSyntaxParser();

    public WebSearchPaneViewModel(PreferencesService preferencesService, DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        SortedSet<SearchBasedFetcher> allFetchers = WebFetchers.getSearchBasedFetchers(preferencesService.getImportFormatPreferences());
        fetchers.setAll(allFetchers);

        // Choose last-selected fetcher as default
        SidePanePreferences sidePanePreferences = preferencesService.getSidePanePreferences();
        int defaultFetcherIndex = sidePanePreferences.getWebSearchFetcherSelected();
        if ((defaultFetcherIndex <= 0) || (defaultFetcherIndex >= fetchers.size())) {
            selectedFetcherProperty().setValue(fetchers.get(0));
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
                    try {
                        parser.parse(queryText, NO_EXPLICIT_FIELD);
                        return null;
                    } catch (ParseException e) {
                        String element = e.currentToken.image;
                        int position = e.currentToken.beginColumn;
                        if (element == null) {
                            return ValidationMessage.error(Localization.lang("Invalid query. Check position %0.", position));
                        } else {
                            return ValidationMessage.error(Localization.lang("Invalid query element '%0' at position %1", element, position));
                        }
                    } catch (QueryNodeParseException e) {
                        return ValidationMessage.error("");
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
        if (StringUtil.isBlank(query)) {
            dialogService.notify(Localization.lang("Please enter a search string"));
            return;
        }
        if (stateManager.getActiveDatabase().isEmpty()) {
            dialogService.notify(Localization.lang("Please open or start a new library before searching"));
            return;
        }

        SearchBasedFetcher activeFetcher = getSelectedFetcher();
        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent("search", Map.of("fetcher", activeFetcher.getName()), Map.of()));

        BackgroundTask<ParserResult> task;
        task = BackgroundTask.wrap(() -> new ParserResult(activeFetcher.performSearch(query)))
                             .withInitialMessage(Localization.lang("Processing %0", query));
        task.onFailure(dialogService::showErrorDialogAndWait);

        ImportEntriesDialog dialog = new ImportEntriesDialog(stateManager.getActiveDatabase().get(), task);
        dialog.setTitle(activeFetcher.getName());
        dialogService.showCustomDialogAndWait(dialog);
    }

    public ValidationStatus queryValidationStatus() {
        return searchQueryValidator.getValidationStatus();
    }
}
