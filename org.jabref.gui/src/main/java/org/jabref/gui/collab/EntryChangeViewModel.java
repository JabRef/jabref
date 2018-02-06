package org.jabref.gui.collab;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntryChangeViewModel extends ChangeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryChangeViewModel.class);

    public EntryChangeViewModel(BibEntry memEntry, BibEntry tmpEntry, BibEntry diskEntry) {
        super();
        Optional<String> key = tmpEntry.getCiteKeyOptional();
        if (key.isPresent()) {
            name = Localization.lang("Modified entry") + ": '" + key.get() + '\'';
        } else {
            name = Localization.lang("Modified entry");
        }

        // We know that tmpEntry is not equal to diskEntry. Check if it has been modified
        // locally as well, since last tempfile was saved.
        boolean isModifiedLocally = (DuplicateCheck.compareEntriesStrictly(memEntry, tmpEntry) <= 1);

        // Another (unlikely?) possibility is that both disk and mem version has been modified
        // in the same way. Check for this, too.
        boolean modificationsAgree = (DuplicateCheck.compareEntriesStrictly(memEntry, diskEntry) > 1);

        LOGGER.debug("Modified entry: " + memEntry.getCiteKeyOptional().orElse("<no BibTeX key set>")
                + "\n Modified locally: " + isModifiedLocally + " Modifications agree: " + modificationsAgree);

        Set<String> allFields = new TreeSet<>();
        allFields.addAll(memEntry.getFieldNames());
        allFields.addAll(tmpEntry.getFieldNames());
        allFields.addAll(diskEntry.getFieldNames());

        for (String field : allFields) {
            Optional<String> mem = memEntry.getField(field);
            Optional<String> tmp = tmpEntry.getField(field);
            Optional<String> disk = diskEntry.getField(field);

            if ((tmp.isPresent()) && (disk.isPresent())) {
                if (!tmp.equals(disk)) {
                    // Modified externally.
                    add(new FieldChangeViewModel(field, memEntry, tmpEntry, mem.orElse(null), tmp.get(), disk.get()));
                }
            } else if (((!tmp.isPresent()) && (disk.isPresent()) && !disk.get().isEmpty())
                    || ((!disk.isPresent()) && (tmp.isPresent()) && !tmp.get().isEmpty()
                            && (mem.isPresent()) && !mem.get().isEmpty())) {
                // Added externally.
                add(new FieldChangeViewModel(field, memEntry, tmpEntry, mem.orElse(null), tmp.orElse(null),
                        disk.orElse(null)));
            }
        }
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        boolean allAccepted = true;

        Enumeration<ChangeViewModel> e = children();
        for (ChangeViewModel c : Collections.list(e)) {
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

    static class FieldChangeViewModel extends ChangeViewModel {

        private final BibEntry entry;
        private final BibEntry tmpEntry;
        private final String field;
        private final String inMem;
        private final String onDisk;
        private final InfoPane tp = new InfoPane();
        private final JScrollPane sp = new JScrollPane(tp);


        public FieldChangeViewModel(String field, BibEntry memEntry, BibEntry tmpEntry, String inMem, String onTmp, String onDisk) {
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
