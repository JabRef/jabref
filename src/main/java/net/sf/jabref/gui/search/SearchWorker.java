package net.sf.jabref.gui.search;

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
    private SearchRule rule;
    private String query = "";
    private SearchMode mode = SearchMode.Incremental;
    private int hits = 0;
    private SearchResultsDialog searchDialog = null;

    /**
     * To keep track of where we are in an incremental search. -1 means that the search is inactive.
     */
    private int incSearchPos = -1;

    public SearchWorker(JabRefFrame frame) {
        this.frame = frame;
    }

    /**
     * Resets the information and display of the previous search.
     * DONE
     */
    public void restart() {

        incSearchPos = -1;
        
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
     * DONE
     * Initializes a new search.
     */
    public void initSearch(SearchRule rule, String query, SearchMode mode) { 
        this.rule = rule;
        if(this.query.equals(query) && this.mode == SearchMode.Incremental) {
            // The query stayed the same and we are in incremental mode
            // So we do not want to start the search at the next item
            incSearchPos ++;
        }
        this.query = query;
        if(this.mode != mode)
        {
            this.mode = mode;
            // We changed search mode so reset information
            restart();
        }
        
        LogFactory.getLog(SearchWorker.class).debug("Search (" +  this.mode.getDisplayName() + "): " + this.query + " at " + incSearchPos);
        
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.Worker#run()
     * DONE
     */
    @Override
    public void run() {

        switch (mode) {
        case Incremental:
            runIncremental();
            break;
        case Float:
        case Filter:
        case LiveFilter:
        case ResultsInDialog:
            runNormal();
            break;
        case Global:
            runGlobal();
            break;
        }
    }

    /**
     * Searches for matches in all open databases. Saves the number of matches in hits. DONE
     */
    private void runGlobal() {
        // Search all databases
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            BasePanel p = frame.baseAt(i);
            for (BibtexEntry entry : p.getDatabase().getEntries()) {

                boolean hit = rule.applyRule(query, entry);
                entry.setSearchHit(hit);
                if (hit) {
                    hits++;
                }
            }
        }
    }

    /**
     * Searches for matches in the current database. Saves the number of matches in hits. DONE
     */
    private void runNormal() {
        // Search the current database
        for (BibtexEntry entry : frame.basePanel().getDatabase().getEntries()) {

            boolean hit = rule.applyRule(query, entry);
            entry.setSearchHit(hit);
            if (hit) {
                hits++;
            }
        }
    }

    /**
     * DONE Searches for the next match, beginning at incSearchPos. The index of the first match is then saved in
     * incSearchPos. Sets it to -1 if no further match was found.
     */
    private void runIncremental() {
        int entryCount = frame.basePanel().getDatabase().getEntryCount();

        if (incSearchPos < 0) {
            incSearchPos = 0;
        }
        if (incSearchPos >= entryCount) {
            incSearchPos = -1;
            return;
        }
        
        for (int i = incSearchPos; i < entryCount; i++) {
            BibtexEntry entry = frame.basePanel().mainTable.getEntryAt(i);
            boolean hit = rule.applyRule(query, entry);
            entry.setSearchHit(hit);
            if (hit) {
                incSearchPos = i;
                return;
            }
        }

        incSearchPos = -1;
        return;
    }

    /**
     * DONE Selects the next match in the entry table based on the position saved in incSearchPos.
     */
    private void updateIncremental() {
        int entryCount = frame.basePanel().getDatabase().getEntryCount();
        if ((incSearchPos >= entryCount) || (incSearchPos < 0)) {
            frame.basePanel().output('\'' + query + "' : " + Localization.lang("Incremental search failed. Repeat to search from top.") + '.');
            return;
        }

        frame.basePanel().selectSingleEntry(incSearchPos);
        frame.basePanel().output('\'' + query + "' " + Localization.lang("found") + '.');
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.AbstractWorker#update()
     * DONE
     */
    @Override
    public void update() {

        // Show the result in the chosen way:
        switch (mode) {
        case Incremental:
            updateIncremental();
            break;
        case Float:
            updateFloat();
            break;
        case Filter:
        case LiveFilter:
            updateFilter();
            break;
        case ResultsInDialog:
            updateResultsInDialog();
            break;
        case Global:
            updateGlobal();
            break;
        }

        if (mode != SearchMode.Incremental) {
            frame.basePanel().output(Localization.lang("Searched database. Number of hits") + ": " + hits);
        }
    }

    /**
     * Floats matches to the top of the entry table. DONE
     */
    private void updateFloat() {
        // TODO: Rename these things in mainTable, they are not search specific
        frame.basePanel().mainTable.showFloatSearch(new SearchMatcher());
        if (hits > 0) {
            frame.basePanel().mainTable.setSelected(0);
        }
    }

    /**
     * Shows only matches in the entry table by removing non-hits. DONE
     */
    private void updateFilter() {
        // TODO: Rename these things in basePanel, they are not search specific
        frame.basePanel().setSearchMatcher(new SearchMatcher());
        if (hits > 0) {
            frame.basePanel().mainTable.setSelected(0);
        }
    }

    /**
     * Displays search results in a dialog window. DONE
     */
    private void updateResultsInDialog() {
        // Make sure the search dialog is instantiated and cleared:
        initSearchDialog();
        searchDialog.clear();
        for (BibtexEntry entry : frame.basePanel().getDatabase().getEntries()) {
            if (entry.isSearchHit()) {
                searchDialog.addEntry(entry, frame.basePanel());
            }
        }
        searchDialog.selectFirstEntry();
        searchDialog.setVisible(true);
    }

    /**
     * Displays search results in a dialog window. DONE
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
     * Initializes the search dialog, unless it has already been instantiated. DONE 
     */
    private void initSearchDialog() {
        // TODO: Move search dialog to main table and make it non-search specific (similar to filter/float by SearchMatcher
        if (searchDialog == null) {
            searchDialog = new SearchResultsDialog(frame, Localization.lang("Search results"));
        }
    }
}