/*  Copyright (C) 2003-2014 JabRef contributors.
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
package net.sf.jabref.journals;

import net.sf.jabref.AbstractWorker;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.undo.NamedCompound;

/**
 * Converts journal abbreviations back to full name for all selected entries.
 */
public class UnabbreviateAction extends AbstractWorker {

    private final BasePanel panel;
    private String message = "";


    public UnabbreviateAction(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void init() {
        panel.output("Unabbreviating...");
    }

    @Override
    public void run() {
        BibtexEntry[] entries = panel.getSelectedEntries();
        if (entries == null) {
            return;
        }

        UndoableUnabbreviator undoableAbbreviator = new UndoableUnabbreviator(Globals.journalAbbrev);

        NamedCompound ce = new NamedCompound("Unabbreviate journal names");
        int count = 0;
        for (BibtexEntry entry : entries) {
            if (undoableAbbreviator.unabbreviate(panel.database(), entry, "journal", ce)) {
                count++;
            }
            if (undoableAbbreviator.unabbreviate(panel.database(), entry, "journaltitle", ce)) {
                count++;
            }
        }
        if (count > 0) {
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
            message = Globals.lang("Unabbreviated %0 journal names.", String.valueOf(count));
        } else {
            message = Globals.lang("No journal names could be unabbreviated.");
        }
    }

    @Override
    public void update() {
        panel.output(message);
    }
}
