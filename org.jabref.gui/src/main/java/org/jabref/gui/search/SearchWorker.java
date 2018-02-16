package org.jabref.gui.search;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.BasePanelMode;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Not reusable. Always create a new instance for each search!
 */
class SearchWorker extends SwingWorker<List<BibEntry>, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchWorker.class);

    private final BasePanel basePanel;
    private final BibDatabase database;

    private final SearchQuery searchQuery;
    private final SearchDisplayMode searchDisplayMode;

    public SearchWorker(BasePanel basePanel, SearchQuery searchQuery, SearchDisplayMode searchDisplayMode) {
        this.basePanel = Objects.requireNonNull(basePanel);
        this.database = Objects.requireNonNull(basePanel.getDatabase());
        this.searchQuery = Objects.requireNonNull(searchQuery);
        this.searchDisplayMode = Objects.requireNonNull(searchDisplayMode);
        LOGGER.debug("Search (" + this.searchDisplayMode.getDisplayName() + "): " + this.searchQuery);
    }

    @Override
    protected List<BibEntry> doInBackground() throws Exception {
        return database.getEntries().parallelStream()
                .filter(searchQuery::isMatch)
                .collect(Collectors.toList());
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            return;
        }

        try {
            updateUIWithSearchResult(get());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("something went wrong during the search", e);
        }
    }

    private void updateUIWithSearchResult(List<BibEntry> matchedEntries) {
        GlobalSearchBar globalSearchBar = JabRefGUI.getMainFrame().getGlobalSearchBar();

        // check if still the current query
        if (!globalSearchBar.isStillValidQuery(searchQuery)) {
            // do not update - another search was already issued
            return;
        }

        // clear
        for (BibEntry entry : basePanel.getDatabase().getEntries()) {
            entry.setSearchHit(false);
        }
        // and mark
        for (BibEntry entry : matchedEntries) {
            entry.setSearchHit(true);
        }

        basePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);
        // Show the result in the chosen way:
        switch (searchDisplayMode) {
            case FLOAT:
                basePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.FLOAT);
                break;
            case FILTER:
                basePanel.getMainTable().getTableModel().updateSearchState(MainTableDataModel.DisplayOption.FILTER);
                break;
            default:
                LOGGER.error("Following searchDisplayMode was not defined: " + searchDisplayMode);
                break;
        }

        // only selects the first match if the selected entries are no hits or no entry is selected
        // and no editor is open (to avoid jumping around when editing an entry)
        if (basePanel.getMode() != BasePanelMode.SHOWING_EDITOR && basePanel.getMode() != BasePanelMode.WILL_SHOW_EDITOR) {
            List<BibEntry> selectedEntries = basePanel.getSelectedEntries();
            boolean isHitSelected = selectedEntries.stream().anyMatch(BibEntry::isSearchHit);
            if (!isHitSelected && !matchedEntries.isEmpty()) {
                for (int i = 0; i < basePanel.getMainTable().getRowCount(); i++) {
                    BibEntry entry = basePanel.getMainTable().getEntryAt(i);
                    if (entry.isSearchHit()) {
                        basePanel.getMainTable().setSelected(i);
                        break;
                    }
                }
            }
        }

        globalSearchBar.updateResults(matchedEntries.size(),
                SearchDescribers.getSearchDescriberFor(searchQuery).getDescription(),
                searchQuery.isGrammarBasedSearch());
        globalSearchBar.getSearchQueryHighlightObservable().fireSearchlistenerEvent(searchQuery);
    }

}
