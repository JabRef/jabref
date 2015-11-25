package net.sf.jabref.gui.search;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class SearchWorker extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(SearchWorker.class);

    private final BasePanel basePanel;

    private final SearchQuery searchQuery;
    private final SearchMode mode;

    private List<BibtexEntry> matchedEntries = new LinkedList<>();
    private int hits = 0;

    public SearchWorker(BasePanel basePanel, SearchQuery searchQuery, SearchMode mode) {
        this.basePanel = Objects.requireNonNull(basePanel);
        this.searchQuery = Objects.requireNonNull(searchQuery);
        this.mode = Objects.requireNonNull(mode);
        LOGGER.debug("Search (" + this.mode.getDisplayName() + "): " + this.searchQuery.toString());
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.Worker#run()
     */
    @Override
    public void run() {
        // clear
        this.hits = 0;
        this.matchedEntries.clear();

        // Search the current database
        for (BibtexEntry entry : basePanel.getDatabase().getEntries()) {
            boolean hit = searchQuery.isMatch(entry);
            if (hit) {
                this.matchedEntries.add(entry);
                hits++;
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.AbstractWorker#update()
     */
    @Override
    public void update() {

        // check if still the current query
        if(!basePanel.getSearchBar().isStillValidQuery(searchQuery)) {
            // do not update - another search was already issued
            return;
        }

        // clear
        for (BibtexEntry entry : basePanel.getDatabase().getEntries()) {
            entry.setSearchHit(false);
        }

        // mark matched
        for(BibtexEntry entry : matchedEntries) {
            entry.setSearchHit(true);
        }

        // resets showing any search results
        if (basePanel.isShowingFloatSearch()) {
            basePanel.mainTable.stopShowingFloatSearch();
        }
        basePanel.stopShowingFilterSearch();

        // Show the result in the chosen way:
        switch (mode) {
        case FLOAT:
            basePanel.stopShowingFilterSearch();
            basePanel.startShowingFloatSearch();
            break;
        case FILTER:
            basePanel.stopShowingFloatSearch();
            basePanel.startShowingFilterSearch();
            break;
        }

        // select first match (i.e., row) if there is any
        if (hits > 0) {
            if (basePanel.mainTable.getRowCount() > 0) {
                basePanel.mainTable.setSelected(0);
            }
        }

        basePanel.getSearchBar().updateResults(hits, searchQuery.description);
        basePanel.getSearchBar().getSearchTextObservable().fireSearchlistenerEvent(searchQuery);
    }

}