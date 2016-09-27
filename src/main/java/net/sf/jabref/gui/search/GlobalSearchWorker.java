package net.sf.jabref.gui.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.SwingWorker;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


class GlobalSearchWorker extends SwingWorker<Map<BasePanel, List<BibEntry>>, Void> {

    private static final Log LOGGER = LogFactory.getLog(GlobalSearchWorker.class);

    private final JabRefFrame frame;
    private final SearchQuery searchQuery;
    private final SearchResultFrame dialog;

    public GlobalSearchWorker(JabRefFrame frame, SearchQuery query) {
        this.frame = Objects.requireNonNull(frame);
        this.searchQuery = Objects.requireNonNull(query);

        dialog = new SearchResultFrame(frame,
                Localization.lang("Search results in all databases for %0",
                        this.searchQuery.localize()),
                searchQuery, true);
        frame.getGlobalSearchBar().setSearchResultFrame(dialog);
    }

    @Override
    protected Map<BasePanel, List<BibEntry>> doInBackground() throws Exception {
        Map<BasePanel, List<BibEntry>> matches = new HashMap<>();
        for (BasePanel basePanel : frame.getBasePanelList()) {
            matches.put(basePanel, basePanel.getDatabase().getEntries().parallelStream()
                    .filter(searchQuery::isMatch)
                    .collect(Collectors.toList()));
        }
        return matches;
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            return;
        }

        try {
            for (Map.Entry<BasePanel, List<BibEntry>> match : get().entrySet()) {
                dialog.addEntries(match.getValue(), match.getKey());
            }
            dialog.selectFirstEntry();
            dialog.setVisible(true);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("something went wrong during the search", e);
        }
    }

}
