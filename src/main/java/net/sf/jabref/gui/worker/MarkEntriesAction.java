/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.worker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.EntryMarker;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 */
public class MarkEntriesAction extends AbstractWorker implements ActionListener {

    private final JabRefFrame frame;
    private final int level;
    private final JMenuItem menuItem;
    private int besLength;

    private static final Log LOGGER = LogFactory.getLog(MarkEntriesAction.class);

    public MarkEntriesAction(JabRefFrame frame, int level) {
        this.frame = frame;
        this.level = level;

        //menuItem = new JMenuItem(Globals.menuTitle("Mark entries").replace("&",""));
        menuItem = new JMenuItem("               ");
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
        List<BibEntry> bes = panel.getSelectedEntries();

        // used at update() to determine output string
        besLength = bes.size();

        if (!bes.isEmpty()) {
            NamedCompound ce = new NamedCompound(Localization.lang("Mark entries"));
            for (BibEntry be : bes) {
                EntryMarker.markEntry(be, level + 1, false, ce);
            }
            ce.end();
            panel.undoManager.addEdit(ce);
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
