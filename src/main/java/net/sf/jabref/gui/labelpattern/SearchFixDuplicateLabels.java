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
package net.sf.jabref.gui.labelpattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableKeyChange;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Function for resolving duplicate BibTeX keys.
 */
public class SearchFixDuplicateLabels extends AbstractWorker {

    private final BasePanel panel;
    private Map<String, List<BibEntry>> dupes;


    public SearchFixDuplicateLabels(BasePanel panel) {

        this.panel = panel;
    }

    @Override
    public void run() {
        // Find all multiple occurences of BibTeX keys.
        dupes = new HashMap<>();

        Map<String, BibEntry> foundKeys = new HashMap<>();
        BibDatabase db = panel.getDatabase();
        for (BibEntry entry : db.getEntries()) {
            String key = entry.getCiteKey();
            // Only handle keys that are actually set:
            if ((key != null) && !key.isEmpty()) {
                // See whether this entry's key is already known:
                if (foundKeys.containsKey(key)) {
                    // Already known, so we have found a dupe. See if it was already found as a dupe:
                    if (dupes.containsKey(key)) {
                        // Already in the dupe map. Add this entry as well:
                        dupes.get(key).add(entry);
                    } else {
                        // Construct a list of entries for this key:
                        List<BibEntry> al = new ArrayList<>();
                        // Add both the first one we found, and the one we found just now:
                        al.add(foundKeys.get(key));
                        al.add(entry);
                        // Add the list to the dupe map:
                        dupes.put(key, al);
                    }
                } else {
                    // Not already known. Add key and entry to map:
                    foundKeys.put(key, entry);
                }
            }
        }
    }

    @Override
    public void init() throws Throwable {
        panel.output(Localization.lang("Resolving duplicate BibTeX keys..."));

    }

    @Override
    public void update() {
        List<BibEntry> toGenerateFor = new ArrayList<>();
        for (Map.Entry<String, List<BibEntry>> dupeEntry : dupes.entrySet()) {
            ResolveDuplicateLabelDialog rdld = new ResolveDuplicateLabelDialog(panel, dupeEntry.getKey(), dupeEntry.getValue());
            rdld.show();
            if (rdld.isOkPressed()) {
                List<JCheckBox> cbs = rdld.getCheckBoxes();
                for (int i = 0; i < cbs.size(); i++) {
                    if (cbs.get(i).isSelected()) {
                        // The checkbox for entry i has been selected, so we should generate a new key for it:
                        toGenerateFor.add(dupeEntry.getValue().get(i));
                    }
                }
            }
        }

        // Do the actual generation:
        if (!toGenerateFor.isEmpty()) {
            NamedCompound ce = new NamedCompound(Localization.lang("Resolve duplicate BibTeX keys"));
            for (BibEntry entry : toGenerateFor) {
                String oldKey = entry.getCiteKey();
                LabelPatternUtil.makeLabel(panel.getBibDatabaseContext().getMetaData(), panel.getDatabase(), entry);
                ce.addEdit(new UndoableKeyChange(panel.getDatabase(), entry, oldKey, entry.getCiteKey()));
            }
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }
        panel.output(Localization.lang("Finished resolving duplicate BibTeX keys. %0 entries modified.",
                String.valueOf(toGenerateFor.size())));
    }
}
