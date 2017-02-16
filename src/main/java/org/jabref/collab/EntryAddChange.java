package org.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.jabref.gui.BasePanel;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.IdGenerator;

class EntryAddChange extends Change {

    private final BibEntry diskEntry;
    private final JScrollPane sp;


    public EntryAddChange(BibEntry diskEntry) {
        super(Localization.lang("Added entry"));
        this.diskEntry = diskEntry;

        PreviewPanel pp = new PreviewPanel(null, diskEntry, null);
        sp = new JScrollPane(pp);
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        diskEntry.setId(IdGenerator.next());
        panel.getDatabase().insertEntry(diskEntry);
        secondary.insertEntry(diskEntry);
        undoEdit.addEdit(new UndoableInsertEntry(panel.getDatabase(), diskEntry, panel));
        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }
}
