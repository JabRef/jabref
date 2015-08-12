/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;

import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

/**
 * This Action may only be used in a menu or button.
 * Never in the entry editor. FileListEditor and EntryEditor have other ways to update the file links
 */
class AutoLinkFilesAction extends AbstractAction {

    public AutoLinkFilesAction() {
        putValue(Action.SMALL_ICON, GUIGlobals.getImage("autoGroup"));
        putValue(Action.NAME, Localization.lang("Automatically set file links"));
        putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey("Automatically link files"));
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        ArrayList<BibtexEntry> entries = new ArrayList<BibtexEntry>();
        Collections.addAll(entries, JabRef.jrf.basePanel().getSelectedEntries());
        if (entries.isEmpty()) {
            JabRef.jrf.basePanel().output(Localization.lang("No entries selected."));
            return;
        }
        JDialog diag = new JDialog(JabRef.jrf, true);
        final NamedCompound nc = new NamedCompound(Localization.lang("Automatically set file links"));
        Runnable runnable = Util.autoSetLinks(entries, nc, null, null, JabRef.jrf.basePanel().metaData(), new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getID() > 0) {
                    // entry has been updated in Util.autoSetLinks, only treat nc and status message
                    if (nc.hasEdits()) {
                        nc.end();
                        JabRef.jrf.basePanel().undoManager.addEdit(nc);
                        JabRef.jrf.basePanel().markBaseChanged();
                    }
                    JabRef.jrf.output(Localization.lang("Finished autosetting external links."));
                } else {
                    JabRef.jrf.output(Localization.lang("Finished autosetting external links.")
                            + " " + Localization.lang("No files found."));
                }
            }
        }, diag);
        JabRefExecutorService.INSTANCE.execute(runnable);
    }
}
