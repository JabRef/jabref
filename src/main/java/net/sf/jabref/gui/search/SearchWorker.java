package net.sf.jabref.gui.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.maintable.MainTableDataModel;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Not reusable. Always create a new instance for each search!
 */
class SearchWorker extends SwingWorker<List<BibEntry>, Void> {

    private static final Log LOGGER = LogFactory.getLog(SearchWorker.class);

    private final BasePanel basePanel;
    private final BibDatabase database;

    private final SearchQuery searchQuery;
    private final SearchMode mode;

    SearchWorker(BasePanel basePanel, SearchQuery searchQuery, SearchMode mode) {
        this.basePanel = Objects.requireNonNull(basePanel);
        this.database = Objects.requireNonNull(basePanel.getDatabase());
        this.searchQuery = Objects.requireNonNull(searchQuery);
        this.mode = Objects.requireNonNull(mode);
        LOGGER.debug("Search (" + this.mode.getDisplayName() + "): " + this.searchQuery);
    }

    @Override
    protected List<BibEntry> doInBackground() throws Exception {
        // Search the current database
        List<BibEntry> matchedEntries = new LinkedList<>();
        matchedEntries.addAll(database.getEntries().stream().filter(searchQuery::isMatch).collect(Collectors.toList()));
        return matchedEntries;
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

        // check if still the current query
        if (!basePanel.getSearchBar().isStillValidQuery(searchQuery)) {
            // do not update - another search was already issued
            return;
        }

        // clear
        for (BibEntry entry : basePanel.getDatabase().getEntries()) {
            entry.setSearchHit(false);
        }

        for (BibEntry entry : matchedEntries) {
            entry.setSearchHit(true);
        }

        basePanel.mainTable.getTableModel().updateSearchState(MainTableDataModel.DisplayOption.DISABLED);

        // Show the result in the chosen way:
        switch (mode) {
        case FLOAT:
            basePanel.mainTable.getTableModel().updateSearchState(MainTableDataModel.DisplayOption.FLOAT);
            break;
        case FILTER:
            basePanel.mainTable.getTableModel().updateSearchState(MainTableDataModel.DisplayOption.FILTER);
            break;
        default:
            break;
        }

        // select first match (i.e., row) if there is any
        int hits = matchedEntries.size();
        if ((hits > 0) && (basePanel.mainTable.getRowCount() > 0)) {
            basePanel.mainTable.setSelected(0);
        }

        basePanel.getSearchBar().updateResults(hits, searchQuery.getDescription(), searchQuery.isGrammarBasedSearch());
        basePanel.getSearchBar().getSearchQueryHighlightObservable().fireSearchlistenerEvent(searchQuery);
    }

}
