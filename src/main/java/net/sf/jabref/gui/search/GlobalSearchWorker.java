package net.sf.jabref.gui.search;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;

import java.util.LinkedList;
import java.util.List;
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
                        this.searchQuery.toString()));
    }

    /* (non-Javadoc)
     * @see net.sf.jabref.Worker#run()
     */
    @Override
    public void run() {
        // Search all databases
        for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
            BasePanel basePanel = frame.baseAt(i);
            for (BibtexEntry entry : basePanel.getDatabase().getEntries()) {
                boolean hit = searchQuery.rule.applyRule(searchQuery.query, entry);
                if (hit) {
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