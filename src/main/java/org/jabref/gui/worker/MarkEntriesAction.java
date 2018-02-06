package org.jabref.gui.worker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.EntryMarker;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkEntriesAction extends AbstractWorker implements ActionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkEntriesAction.class);
    private final JabRefFrame frame;
    private final int level;
    private final JMenuItem menuItem;

    private int besLength;

    public MarkEntriesAction(JabRefFrame frame, int level) {
        this.frame = frame;
        this.level = level;

        //menuItem = new JMenuItem(Globals.menuTitle("Mark entries").replace("&",""));
        menuItem = new JMenuItem(Localization.lang("Level") + " " + level + "   ");
        menuItem.setMnemonic(String.valueOf(level + 1).charAt(0));
        menuItem.setBackground(Globals.prefs.getColor(JabRefPreferences.MARKED_ENTRY_BACKGROUND + this.level));
        menuItem.setOpaque(true);
        menuItem.addActionListener(this);
    }

    public JMenuItem getMenuItem() {
        return menuItem;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            this.init();
            getWorker().run();
            getCallBack().update();
        } catch (Throwable t) {
            LOGGER.warn("Problem marking entries", t);
        }
    }

    @Override
    public void run() {
        BasePanel panel = frame.getCurrentBasePanel();
        if (panel != null) {
            List<BibEntry> bes = panel.getSelectedEntries();

            // used at update() to determine output string
            besLength = bes.size();

            if (!bes.isEmpty()) {
                NamedCompound ce = new NamedCompound(Localization.lang("Mark entries"));
                for (BibEntry be : bes) {
                    EntryMarker.markEntry(be, level + 1, false, ce);
                }
                ce.end();
                panel.getUndoManager().addEdit(ce);
            }
        }
    }

    @Override
    public void update() {
        String outputStr;
        switch (besLength) {
        case 0:
            outputStr = Localization.lang("This operation requires one or more entries to be selected.");
            break;
        case 1:
            frame.getCurrentBasePanel().markBaseChanged();
            outputStr = Localization.lang("Marked selected entry");
            break;
        default:
            frame.getCurrentBasePanel().markBaseChanged();
            outputStr = Localization.lang("Marked all %0 selected entries", Integer.toString(besLength));
            break;
        }
        frame.output(outputStr);
    }
}
