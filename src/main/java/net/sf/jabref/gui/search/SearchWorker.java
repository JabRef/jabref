package net.sf.jabref.gui.search;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Not reusable. Always create a new instance for each search!
 */
class SearchWorker extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(SearchWorker.class);

    private final BasePanel basePanel;
    private final BibDatabase database;

    private final SearchQuery searchQuery;
    private final SearchMode mode;

    private final List<BibEntry> matchedEntries = new LinkedList<>();

    public SearchWorker(BasePanel basePanel, SearchQuery searchQuery, SearchMode mode) {
        this.basePanel = Objects.requireNonNull(basePanel);
        this.database = Objects.requireNonNull(basePanel.getDatabase());
        this.searchQuery = Objects.requireNonNull(searchQuery);
        this.mode = Objects.requireNonNull(mode);
        LOGGER.debug("Search (" + this.mode.getDisplayName() + "): " + this.searchQuery);
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.Worker#run()
     */
    @Override
    public void run() {
        // Search the current database
        this.matchedEntries.addAll(database.getEntries().stream().filter(searchQuery::isMatch).collect(Collectors.toList()));
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
        for (BibEntry entry : basePanel.getDatabase().getEntries()) {
            entry.setSearchHit(false);
        }

        for (BibEntry entry : this.matchedEntries) {
            entry.setSearchHit(true);
        }

        basePanel.stopShowingFloatSearch();
        basePanel.getFilterSearchToggle().stop();

        // Show the result in the chosen way:
        switch (mode) {
        case FLOAT:
            basePanel.getFilterSearchToggle().stop();
            basePanel.startShowingFloatSearch();
            break;
        case FILTER:
            basePanel.stopShowingFloatSearch();
            basePanel.getFilterSearchToggle().start();
            break;
        }

        // select first match (i.e., row) if there is any
        int hits = this.matchedEntries.size();
        if (hits > 0) {
            if (basePanel.mainTable.getRowCount() > 0) {
                basePanel.mainTable.setSelected(0);
            }
        }

        basePanel.getSearchBar().updateResults(hits, searchQuery.description, searchQuery.isGrammarBasedSearch());
        basePanel.getSearchBar().getSearchTextObservable().fireSearchlistenerEvent(searchQuery);
    }

}