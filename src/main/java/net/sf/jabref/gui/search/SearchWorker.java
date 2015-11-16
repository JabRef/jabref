package net.sf.jabref.gui.search;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.search.matchers.SearchMatcher;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.LogFactory;

import java.util.Objects;

class SearchWorker extends AbstractWorker {

    private final BasePanel basePanel;

    private SearchQuery searchQuery;
    private SearchMode mode = SearchMode.FILTER;

    private int hits = 0;

    public SearchWorker(BasePanel basePanel) {
        this.basePanel = Objects.requireNonNull(basePanel);
    }

    /**
     * Resets the information and display of the previous search.
     */
    public void restart() {
        if (basePanel.isShowingFloatSearch()) {
            basePanel.mainTable.stopShowingFloatSearch();
        }
        if (basePanel.isShowingFilterSearch()) {
            basePanel.stopShowingSearchResults();
        }
    }

    /**
     * Initializes a new search.
     */
    public void initSearch(SearchQuery searchQuery, SearchMode mode) {
        this.searchQuery = searchQuery;
        if (this.mode != mode) {
            this.mode = mode;
            // We changed search mode so reset information
            restart();
        }

        LogFactory.getLog(SearchWorker.class).debug("Search (" + this.mode.getDisplayName() + "): " + this.searchQuery.toString());
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.Worker#run()
     */
    @Override
    public void run() {
        this.hits = 0;

        runNormal();
    }

    /**
     * Searches for matches in the current database. Saves the number of matches in hits.
     */
    private void runNormal() {
        // Search the current database
        for (BibtexEntry entry : basePanel.getDatabase().getEntries()) {

            boolean hit = searchQuery.rule.applyRule(searchQuery.query, entry);
            entry.setSearchHit(hit);
            if (hit) {
                hits++;
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.AbstractWorker#update()
     */
    @Override
    public void update() {

        // Show the result in the chosen way:
        switch (mode) {
        case FLOAT:
            updateFloat();
            break;
        case FILTER:
            updateFilter();
            break;
        }

        basePanel.getSearchBar().updateResults(hits, searchQuery.description);
    }

    /**
     * Floats matches to the top of the entry table.
     */
    private void updateFloat() {
        basePanel.mainTable.showFloatSearch(new SearchMatcher());
        if (hits > 0) {
            if (basePanel.mainTable.getRowCount() > 0) {
                basePanel.mainTable.setSelected(0);
            }
        }
    }

    /**
     * Shows only matches in the entry table by removing non-hits.
     */
    private void updateFilter() {
        basePanel.setSearchMatcher(new SearchMatcher());
        if (hits > 0) {
            if (basePanel.mainTable.getRowCount() > 0) {
                basePanel.mainTable.setSelected(0);
            }
        }
    }
}