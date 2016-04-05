/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.external.push;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sf.jabref.*;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

/**
 * An Action class representing the process of invoking a PushToApplication operation.
 */
class PushToApplicationAction extends AbstractAction implements Runnable {
    private final PushToApplication operation;
    private final JabRefFrame frame;
    private BasePanel panel;
    private List<BibEntry> entries;


    public PushToApplicationAction(JabRefFrame frame, PushToApplication operation) {
        this.frame = frame;
        putValue(Action.SMALL_ICON, operation.getIcon());
        putValue(Action.NAME, operation.getName());
        putValue(Action.SHORT_DESCRIPTION, operation.getTooltip());
        this.operation = operation;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        panel = frame.getCurrentBasePanel();

        // Check if a BasePanel exists:
        if (panel == null) {
            return;
        }

        // Check if any entries are selected:
        entries = panel.getSelectedEntries();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("This operation requires one or more entries to be selected."), (String) getValue(Action.NAME), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // If required, check that all entries have BibTeX keys defined:
        if (operation.requiresBibtexKeys()) {
            for (BibEntry entry : entries) {
                if ((entry.getCiteKey() == null) || entry.getCiteKey().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(frame,
                            Localization
                                    .lang("This operation requires all selected entries to have BibTeX keys defined."),
                            (String) getValue(Action.NAME), JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        // All set, call the operation in a new thread:
        JabRefExecutorService.INSTANCE.execute(this);
    }

    @Override
    public void run() {
        // Do the operation:
        operation.pushEntries(panel.getDatabase(), entries, getKeyString(entries), panel.getBibDatabaseContext().getMetaData());

        // Call the operationCompleted() method on the event dispatch thread:
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                operation.operationCompleted(panel);
            }
        });
    }

    private static String getKeyString(List<BibEntry> bibentries) {
        StringBuilder result = new StringBuilder();
        String citeKey;
        boolean first = true;
        for (BibEntry bes : bibentries) {
            citeKey = bes.getCiteKey();
            // if the key is empty we give a warning and ignore this entry
            if ((citeKey == null) || citeKey.isEmpty()) {
                continue;
            }
            if (first) {
                result.append(citeKey);
                first = false;
            } else {
                result.append(',').append(citeKey);
            }
        }
        return result.toString();
    }
}
