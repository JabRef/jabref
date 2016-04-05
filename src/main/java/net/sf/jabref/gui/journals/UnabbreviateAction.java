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
package net.sf.jabref.gui.journals;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import java.util.List;

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
        panel.output(Localization.lang("Unabbreviating..."));
    }

    @Override
    public void run() {
        List<BibEntry> entries = panel.getSelectedEntries();
        if (entries == null) {
            return;
        }

        UndoableUnabbreviator undoableAbbreviator = new UndoableUnabbreviator(
                Globals.journalAbbreviationLoader.getRepository());

        NamedCompound ce = new NamedCompound(Localization.lang("Unabbreviate journal names"));
        int count = 0;
        for (BibEntry entry : entries) {
            if (undoableAbbreviator.unabbreviate(panel.getDatabase(), entry, "journal", ce)) {
                count++;
            }
            if (undoableAbbreviator.unabbreviate(panel.getDatabase(), entry, "journaltitle", ce)) {
                count++;
            }
        }
        if (count > 0) {
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
            message = Localization.lang("Unabbreviated %0 journal names.", String.valueOf(count));
        } else {
            message = Localization.lang("No journal names could be unabbreviated.");
        }
    }

    @Override
    public void update() {
        panel.output(message);
    }
}
