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
package net.sf.jabref.labelPattern;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableKeyChange;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Function for resolving duplicate BibTeX keys.
 */
public class SearchFixDuplicateLabels extends AbstractWorker {
    private BasePanel panel;
    HashMap<String, List<BibtexEntry>> dupes = null;

    public SearchFixDuplicateLabels(BasePanel panel) {

        this.panel = panel;
    }

    public void run() {
        // Find all multiple occurences of BibTeX keys.
        dupes = new HashMap<String, List<BibtexEntry>>();

        HashMap<String, BibtexEntry> foundKeys = new HashMap<String, BibtexEntry>();
        BibtexDatabase db = panel.database();
        for (BibtexEntry entry : db.getEntries()) {
            String key = entry.getCiteKey();
            // Only handle keys that are actually set:
            if ((key != null) && (key.length() > 0)) {
                // See whether this entry's key is already known:
                if (!foundKeys.containsKey(key)) {
                    // Not already known. Add key and entry to map:
                    foundKeys.put(key, entry);
                }
                else {
                    // Already known, so we have found a dupe. See if it was already found as a dupe:
                    if (dupes.containsKey(key)) {
                        // Already in the dupe map. Add this entry as well:
                        dupes.get(key).add(entry);
                    }
                    else {
                        // Construct a list of entries for this key:
                        ArrayList<BibtexEntry> al = new ArrayList<BibtexEntry>();
                        // Add both the first one we found, and the one we found just now:
                        al.add(foundKeys.get(key));
                        al.add(entry);
                        // Add the list to the dupe map:
                        dupes.put(key, al);
                    }
                }
            }
        }
    }

    @Override
    public void init() throws Throwable {
        panel.output(Globals.lang("Resolving duplicate BibTeX keys..."));

    }

    @Override
    public void update() {
        List<BibtexEntry> toGenerateFor = new ArrayList<BibtexEntry>();
        for (String key : dupes.keySet()) {
            ResolveDuplicateLabelDialog rdld = new ResolveDuplicateLabelDialog(panel,
                    key, dupes.get(key));
            rdld.show();
            if (rdld.isOkPressed()) {
                List<JCheckBox> cbs = rdld.getCheckBoxes();
                for (int i=0; i<cbs.size(); i++) {
                    if (cbs.get(i).isSelected()) {
                        // The checkbox for entry i has been selected, so we should generate a new key for it:
                        toGenerateFor.add(dupes.get(key).get(i));
                    }
                }
            }
        }

        // Do the actual generation:
        if (toGenerateFor.size() > 0) {
            NamedCompound ce = new NamedCompound("resolve duplicate keys");
            for (BibtexEntry entry : toGenerateFor) {
                String oldKey = entry.getCiteKey();
                entry = LabelPatternUtil.makeLabel(panel.metaData(), panel.database(), entry);
                ce.addEdit(new UndoableKeyChange(panel.database(), entry.getId(), oldKey,
                    entry.getField(BibtexFields.KEY_FIELD)));
            }
            ce.end();
            panel.undoManager.addEdit(ce);
            panel.markBaseChanged();
        }
        panel.output(Globals.lang("Finished resolving duplicate BibTeX keys. %0 entries modified.",
                String.valueOf(toGenerateFor.size())));
    }
}
