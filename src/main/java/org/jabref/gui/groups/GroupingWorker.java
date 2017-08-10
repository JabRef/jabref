package org.jabref.gui.groups;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;
import org.jabref.preferences.JabRefPreferences;

class GroupingWorker {

    private final JabRefFrame frame;
    private final BasePanel panel;

    public GroupingWorker(JabRefFrame frame, BasePanel panel) {
        this.frame = frame;
        this.panel = panel;
    }

    public void run(SearchMatcher matcher) {
        for (BibEntry entry : panel.getDatabase().getEntries()) {
            boolean hit = matcher.isMatch(entry);
            entry.setGroupHit(hit);
        }
    }

    public void update() {
        // Show the result in the chosen way:
        if (Globals.prefs.getBoolean(JabRefPreferences.GRAY_OUT_NON_HITS)) {
            panel.getMainTable().getTableModel().updateGroupingState(MainTableDataModel.DisplayOption.FLOAT);
        } else {
            panel.getMainTable().getTableModel().updateGroupingState(MainTableDataModel.DisplayOption.FILTER);
        }
        panel.getMainTable().getTableModel().updateSortOrder();
        panel.getMainTable().getTableModel().updateGroupFilter();
        panel.getMainTable().scrollTo(0);

        frame.output(Localization.lang("Updated group selection") + ".");
    }
}
