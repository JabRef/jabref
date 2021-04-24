package org.jabref.gui.importer.fetcher;

import java.util.Collections;
import java.util.Optional;
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
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class WebSearchPaneViewModel {

    private final ObjectProperty<SearchBasedFetcher> selectedFetcher = new SimpleObjectProperty<>();
    private final ListProperty<SearchBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty query = new SimpleStringProperty();
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final DoiFetcher doiFetcher;
    private final IsbnFetcher isbnFetcher;

    public WebSearchPaneViewModel(PreferencesService preferencesService, DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        SortedSet<SearchBasedFetcher> allFetchers = WebFetchers.getSearchBasedFetchers(preferencesService.getImportFormatPreferences());
        fetchers.setAll(allFetchers);

        // Choose last-selected fetcher as default
        int defaultFetcherIndex = preferencesService.getSidePanePreferences().getWebSearchFetcherSelected();
        if ((defaultFetcherIndex <= 0) || (defaultFetcherIndex >= fetchers.size())) {
            selectedFetcherProperty().setValue(fetchers.get(0));
        } else {
            selectedFetcherProperty().setValue(fetchers.get(defaultFetcherIndex));
        }
        EasyBind.subscribe(selectedFetcherProperty(), newFetcher -> {
            int newIndex = fetchers.indexOf(newFetcher);
            preferencesService.storeSidePanePreferences(preferencesService.getSidePanePreferences().withWebSearchFetcherSelected(newIndex));
        });

        this.doiFetcher = new DoiFetcher(preferencesService.getImportFormatPreferences());
        this.isbnFetcher = new IsbnFetcher(preferencesService.getImportFormatPreferences());
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
        if (StringUtil.isBlank(getQuery())) {
            dialogService.notify(Localization.lang("Please enter a search string"));
            return;
        }

        if (stateManager.getActiveDatabase().isEmpty()) {
            dialogService.notify(Localization.lang("Please open or start a new library before searching"));
            return;
        }

        SearchBasedFetcher activeFetcher = getSelectedFetcher();

        BackgroundTask<ParserResult> task;
        task = BackgroundTask.wrap(() -> {
            Optional<DOI> doi = DOI.parse(getQuery());
            if (doi.isPresent()) {
                try {
                    Optional<BibEntry> bibEntry = doiFetcher.performSearchById(doi.get().getDOI());
                    if (bibEntry.isPresent()) {
                        return new ParserResult(Collections.singletonList(bibEntry.get()));
                    }
                } catch (FetcherException ignore) {
                    // Ignored, because it can be something else
                }
            }

            Optional<ISBN> isbn = ISBN.parse(getQuery());
            if (isbn.isPresent()) {
                try {
                    Optional<BibEntry> bibEntry = isbnFetcher.performSearchById(isbn.get().getNormalized());
                    if (bibEntry.isPresent()) {
                        return new ParserResult(Collections.singletonList(bibEntry.get()));
                    }
                } catch (FetcherException ignore) {
                    // Ignored, because it can be something els
                }
            }

            return new ParserResult(activeFetcher.performSearch(getQuery().trim()));
        }).withInitialMessage(Localization.lang("Processing %0", getQuery().trim()));
        
        task.onFailure(dialogService::showErrorDialogAndWait);

        ImportEntriesDialog dialog = new ImportEntriesDialog(stateManager.getActiveDatabase().get(), task);
        dialog.setTitle(activeFetcher.getName());
        dialogService.showCustomDialogAndWait(dialog);
    }
}
