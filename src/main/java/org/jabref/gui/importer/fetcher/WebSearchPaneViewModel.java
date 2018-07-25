package org.jabref.gui.importer.fetcher;

import java.util.Comparator;
import java.util.List;

import javax.swing.SwingUtilities;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.JabRefExecutorService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSearchPaneViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSearchPaneViewModel.class);

    private final ObjectProperty<SearchBasedFetcher> selectedFetcher = new SimpleObjectProperty<>();
    private final ListProperty<SearchBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty query = new SimpleStringProperty();
    private final JabRefFrame frame;

    public WebSearchPaneViewModel(ImportFormatPreferences importPreferences, JabRefFrame frame, JabRefPreferences preferences) {
        // TODO: Rework so that we don't rely on JabRefFrame and not the complete preferences
        this.frame = frame;

        List<SearchBasedFetcher> allFetchers = WebFetchers.getSearchBasedFetchers(importPreferences);
        allFetchers.sort(Comparator.comparing(WebFetcher::getName));
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
            frame.output(Localization.lang("Please enter a search string"));
            return;
        }

        if (frame.getCurrentBasePanel() == null) {
            frame.output(Localization.lang("Please open or start a new library before searching"));
            return;
        }

        SearchBasedFetcher activeFetcher = getSelectedFetcher();
        final ImportInspectionDialog dialog = new ImportInspectionDialog(frame, frame.getCurrentBasePanel(),
                activeFetcher.getName(), false);

        SwingUtilities.invokeLater(() -> dialog.setVisible(true));

        JabRefExecutorService.INSTANCE.execute(() -> {
            dialog.setStatus(Localization.lang("Processing %0", getQuery()));
            try {
                List<BibEntry> matches = activeFetcher.performSearch(getQuery().trim());
                dialog.addEntries(matches);
                dialog.entryListComplete();
            } catch (FetcherException e) {
                LOGGER.error("Error while fetching from " + activeFetcher.getName(), e);
                dialog.showErrorMessage(activeFetcher.getName(), e.getLocalizedMessage());
            }
        });
    }
}
