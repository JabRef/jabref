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
package net.sf.jabref.collab;

import java.util.Collections;
import java.util.Enumeration;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.model.DuplicateCheck;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

class EntryChange extends Change {

    private static final Log LOGGER = LogFactory.getLog(EntryChange.class);

    public EntryChange(BibEntry memEntry, BibEntry tmpEntry, BibEntry diskEntry) {
        super();
        String key = tmpEntry.getCiteKey();
        if (key == null) {
            name = Localization.lang("Modified entry");
        } else {
            name = Localization.lang("Modified entry") + ": '" + key + '\'';
        }

        // We know that tmpEntry is not equal to diskEntry. Check if it has been modified
        // locally as well, since last tempfile was saved.
        boolean isModifiedLocally = (DuplicateCheck.compareEntriesStrictly(memEntry, tmpEntry) <= 1);

        // Another (unlikely?) possibility is that both disk and mem version has been modified
        // in the same way. Check for this, too.
        boolean modificationsAgree = (DuplicateCheck.compareEntriesStrictly(memEntry, diskEntry) > 1);

        LOGGER.debug("Modified entry: " + memEntry.getCiteKey() + "\n Modified locally: " + isModifiedLocally
                + " Modifications agree: " + modificationsAgree);

        TreeSet<String> allFields = new TreeSet<>();
        allFields.addAll(memEntry.getFieldNames());
        allFields.addAll(tmpEntry.getFieldNames());
        allFields.addAll(diskEntry.getFieldNames());

        for (String field : allFields) {
            String mem = memEntry.getField(field);
            String tmp = tmpEntry.getField(field);
            String disk = diskEntry.getField(field);

            if ((tmp != null) && (disk != null)) {
                if (!tmp.equals(disk)) {
                    // Modified externally.
                    add(new FieldChange(field, memEntry, tmpEntry, mem, tmp, disk));
                }
            } else if (((tmp == null) && (disk != null) && !disk.isEmpty()) || ((disk == null) && (tmp != null) && !tmp.isEmpty()
                    && (mem != null) && !mem.isEmpty())) {
                // Added externally.
                add(new FieldChange(field, memEntry, tmpEntry, mem, tmp, disk));
            }
        }
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        boolean allAccepted = true;

        Enumeration<Change> e = children();
        for (Change c : Collections.list(e)) {
            if (c.isAcceptable() && c.isAccepted()) {
                c.makeChange(panel, secondary, undoEdit);
            } else {
                allAccepted = false;
            }
        }

        /*panel.database().removeEntry(memEntry.getId());
        try {
          diskEntry.setId(Util.next());
        } catch (KeyCollisionException ex) {}
        panel.database().removeEntry(memEntry.getId());*/

        return allAccepted;
    }

    @Override
    public JComponent description() {
        return new JLabel(name);
    }


    static class FieldChange extends Change {

        private final BibEntry entry;
        private final BibEntry tmpEntry;
        private final String field;
        private final String inMem;
        private final String onDisk;
        private final InfoPane tp = new InfoPane();
        private final JScrollPane sp = new JScrollPane(tp);


        public FieldChange(String field, BibEntry memEntry, BibEntry tmpEntry, String inMem, String onTmp, String onDisk) {
            super(field);
            entry = memEntry;
            this.tmpEntry = tmpEntry;
            this.field = field;
            this.inMem = inMem;
            this.onDisk = onDisk;

            StringBuilder text = new StringBuilder(36);
            text.append("<FONT SIZE=10><H2>").append(Localization.lang("Modification of field"))
                    .append(" <I>").append(field).append("</I></H2>");

            if ((onDisk != null) && !onDisk.isEmpty()) {
                text.append("<H3>").append(Localization.lang("Value set externally")).append(":</H3> ").append(onDisk);
            } else {
                text.append("<H3>").append(Localization.lang("Value cleared externally")).append("</H3>");
            }

            if ((inMem != null) && !inMem.isEmpty()) {
                text.append("<H3>").append(Localization.lang("Current value")).append(":</H3> ").append(inMem);
            }
            if ((onTmp != null) && !onTmp.isEmpty()) {
                text.append("<H3>").append(Localization.lang("Current tmp value")).append(":</H3> ").append(onTmp);
            }
            tp.setContentType("text/html");
            tp.setText(text.toString());
        }

        @Override
        public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
            if (onDisk == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, onDisk);
            }
            undoEdit.addEdit(new UndoableFieldChange(entry, field, inMem, onDisk));
            if (onDisk == null) {
                tmpEntry.clearField(field);
            } else {
                tmpEntry.setField(field, onDisk);
            }
            return true;
        }

        @Override
        public JComponent description() {
            return sp;
        }

    }
}
