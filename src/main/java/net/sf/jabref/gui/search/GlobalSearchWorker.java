package net.sf.jabref.gui.search;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.search.SearchQueryLocalizer;
import net.sf.jabref.model.entry.BibtexEntry;

import java.util.Objects;

class GlobalSearchWorker extends AbstractWorker {

    private final JabRefFrame frame;
    private final SearchQuery searchQuery;
    private final SearchResultsDialog dialog;

    public GlobalSearchWorker(JabRefFrame frame, SearchQuery query) {
        this.frame = Objects.requireNonNull(frame);
        this.searchQuery = Objects.requireNonNull(query);

        dialog = new SearchResultsDialog(frame,
                Localization.lang("Search results in all databases for %0",
                        SearchQueryLocalizer.localize(this.searchQuery)));
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.Worker#run()
     */
    @Override
    public void run() {
        // Search all databases
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            BasePanel basePanel = frame.getBasePanelAt(i);
            for (BibtexEntry entry : basePanel.getDatabase().getEntries()) {
                if (searchQuery.isMatch(entry)) {
                    dialog.addEntry(entry, basePanel);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.AbstractWorker#update()
     */
    @Override
    public void update() {
        dialog.selectFirstEntry();
        dialog.setVisible(true);
    }

}