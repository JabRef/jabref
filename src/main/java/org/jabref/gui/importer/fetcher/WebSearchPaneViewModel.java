package org.jabref.gui.importer.fetcher;

import java.util.List;
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
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import org.fxmisc.easybind.EasyBind;

public class WebSearchPaneViewModel {

    private final ObjectProperty<SearchBasedFetcher> selectedFetcher = new SimpleObjectProperty<>();
    private final ListProperty<SearchBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty query = new SimpleStringProperty();
    private final JabRefFrame frame;
    private final DialogService dialogService;

    public WebSearchPaneViewModel(ImportFormatPreferences importPreferences, JabRefFrame frame, JabRefPreferences preferences, DialogService dialogService) {
        // TODO: Rework so that we don't rely on JabRefFrame and not the complete preferences
        this.frame = frame;
        this.dialogService = dialogService;

        SortedSet<SearchBasedFetcher> allFetchers = WebFetchers.getSearchBasedFetchers(importPreferences);
        fetchers.setAll(allFetchers);

        // Choose last-selected fetcher as default
        int defaultFetcherIndex = preferences.getInt(JabRefPreferences.SELECTED_FETCHER_INDEX);
        if ((defaultFetcherIndex <= 0) || (defaultFetcherIndex >= fetchers.size())) {
            selectedFetcherProperty().setValue(fetchers.get(0));
        } else {
            selectedFetcherProperty().setValue(fetchers.get(defaultFetcherIndex));
        }
        EasyBind.subscribe(selectedFetcherProperty(), newFetcher -> {
            int newIndex = fetchers.indexOf(newFetcher);
            preferences.putInt(JabRefPreferences.SELECTED_FETCHER_INDEX, newIndex);
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
        if (StringUtil.isBlank(getQuery())) {
            dialogService.notify(Localization.lang("Please enter a search string"));
            return;
        }

        if (frame.getCurrentBasePanel() == null) {
            dialogService.notify(Localization.lang("Please open or start a new library before searching"));
            return;
        }

        SearchBasedFetcher activeFetcher = getSelectedFetcher();

        BackgroundTask<List<BibEntry>> task = BackgroundTask.wrap(() -> activeFetcher.performSearch(getQuery().trim()))
                                                            .withInitialMessage(Localization.lang("Processing %0", getQuery()));

        task.onFailure(dialogService::showErrorDialogAndWait);

        ImportEntriesDialog dialog = new ImportEntriesDialog(frame.getCurrentBasePanel().getBibDatabaseContext(), task);
        dialog.setTitle(activeFetcher.getName());
        dialog.showAndWait();
    }
}
