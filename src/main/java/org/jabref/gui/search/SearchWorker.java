package org.jabref.gui.search;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.search.rules.describer.SearchDescribers;
import org.jabref.gui.util.DefaultTaskExecutor;
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

        DefaultTaskExecutor.runInJavaFXThread(() ->
                globalSearchBar.updateResults(matchedEntries.size(),
                        SearchDescribers.getSearchDescriberFor(searchQuery).getDescription(),
                        searchQuery.isGrammarBasedSearch()));
        globalSearchBar.getSearchQueryHighlightObservable().fireSearchlistenerEvent(searchQuery);
    }

}
