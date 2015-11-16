package net.sf.jabref.gui.search;

import net.sf.jabref.logic.search.describer.SearchDescribers;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchRule;
import net.sf.jabref.logic.search.matchers.SearchMatcher;

class SearchWorker extends AbstractWorker {

    private final JabRefFrame frame;

    private SearchQuery searchQuery;
    private SearchMode mode = SearchMode.FILTER;

    private int hits = 0;

    private SearchResultsDialog searchDialog = null;

    public SearchWorker(JabRefFrame frame) {
        this.frame = frame;
    }

    /**
     * Resets the information and display of the previous search.
     */
    public void restart() {

        if (frame.basePanel() == null) {
            return;
        }

        if (frame.basePanel().isShowingFloatSearch()) {
            frame.basePanel().mainTable.stopShowingFloatSearch();
        } 
        if (frame.basePanel().isShowingFilterSearch()) {
            frame.basePanel().stopShowingSearchResults();
        }
    }

    /**
     * Initializes a new search.
     */
    public void initSearch(SearchQuery searchQuery, SearchMode mode) {
        this.searchQuery = searchQuery;
        if(this.mode != mode) {
            this.mode = mode;
            // We changed search mode so reset information
            restart();
        }
        
        LogFactory.getLog(SearchWorker.class).debug("Search (" +  this.mode.getDisplayName() + "): " + this.searchQuery.toString());
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.Worker#run()
     */
    @Override
    public void run() {
        this.hits = 0;

        switch (mode) {
        case FLOAT:
        case FILTER:
            runNormal();
            break;
        case GLOBAL:
            runGlobal();
            break;
        }
    }

    /**
     * Searches for matches in all open databases. Saves the number of matches in hits.
     */
    private void runGlobal() {
        // Search all databases
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            findResultsInBasePanel(frame.baseAt(i));
        }
    }

    private void findResultsInBasePanel(BasePanel p) {
        for (BibtexEntry entry : p.getDatabase().getEntries()) {

            boolean hit = searchQuery.rule.applyRule(searchQuery.query, entry);
            entry.setSearchHit(hit);
            if (hit) {
                hits++;
            }
        }
    }

    /**
     * Searches for matches in the current database. Saves the number of matches in hits.
     */
    private void runNormal() {
        // Search the current database
        findResultsInBasePanel(frame.basePanel());
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
        case GLOBAL:
            updateGlobal();
            break;
        }

        frame.getSearchBar().updateResults(hits, searchQuery.description);
    }

    /**
     * Floats matches to the top of the entry table.
     */
    private void updateFloat() {
        // TODO: Rename these things in mainTable, they are not search specific
        frame.basePanel().mainTable.showFloatSearch(new SearchMatcher());
        if (hits > 0) {
            if(frame.basePanel().mainTable.getRowCount() > 0) {
                frame.basePanel().mainTable.setSelected(0);
            }
        }
    }

    /**
     * Shows only matches in the entry table by removing non-hits.
     */
    private void updateFilter() {
        // TODO: Rename these things in basePanel, they are not search specific
        frame.basePanel().setSearchMatcher(new SearchMatcher());
        if (hits > 0) {
            if(frame.basePanel().mainTable.getRowCount() > 0) {
                frame.basePanel().mainTable.setSelected(0);
            }
        }
    }

    /**
     * Displays search results in a dialog window.
     */
    private void updateGlobal() {
        // Make sure the search dialog is instantiated and cleared:
        initSearchDialog();
        searchDialog.clear();
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            BasePanel p = frame.baseAt(i);
            for (BibtexEntry entry : p.getDatabase().getEntries()) {
                if (entry.isSearchHit()) {
                    searchDialog.addEntry(entry, p);
                }
            }
        }
        searchDialog.selectFirstEntry();
        searchDialog.setVisible(true);
    }

    /**
     * Initializes the search dialog, unless it has already been instantiated.
     */
    private void initSearchDialog() {
        // TODO: Move search dialog to main table and make it non-search specific (similar to filter/float by SearchMatcher
        if (searchDialog == null) {
            searchDialog = new SearchResultsDialog(frame, Localization.lang("Search results"));
        }
    }
}