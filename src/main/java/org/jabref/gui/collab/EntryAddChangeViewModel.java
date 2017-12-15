package org.jabref.gui.collab;

import javax.swing.JComponent;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.gui.BasePanel;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntry;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.IdGenerator;

class EntryAddChangeViewModel extends ChangeViewModel {

    private final BibEntry diskEntry;
    private final JFXPanel container;


    public EntryAddChangeViewModel(BibEntry diskEntry) {
        super(Localization.lang("Added entry"));
        this.diskEntry = diskEntry;

        PreviewPanel previewPanel = new PreviewPanel(null, null);
        previewPanel.setEntry(diskEntry);
        container = CustomJFXPanel.wrap(new Scene(previewPanel));
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
        return container;
    }
}
