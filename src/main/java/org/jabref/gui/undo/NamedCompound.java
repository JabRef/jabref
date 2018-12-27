package org.jabref.gui.undo;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.jabref.logic.l10n.Localization;

public class NamedCompound extends CompoundEdit {

    private final String name;
    private boolean hasEdits;


    public NamedCompound(String name) {
        super();
        this.name = name;
    }

    @Override
    public boolean addEdit(UndoableEdit undoableEdit) {
        hasEdits = true;
        return super.addEdit(undoableEdit);
    }

    public boolean hasEdits() {
        return hasEdits;
    }

    @Override
    public String getUndoPresentationName() {
        return "<html>" + Localization.lang("Undo") + ": " + name + "<ul>" + getPresentationName() + "</ul></html>";
    }

    @Override
    public String getRedoPresentationName() {
        return "<html>" + Localization.lang("Redo") + ": " + name + "<ul>" + getPresentationName() + "</ul></html>";
    }

    @Override
    public String getPresentationName() {
        StringBuilder sb = new StringBuilder();
        for (UndoableEdit edit : edits) {
            sb.append("<li>").append(edit.getPresentationName());
        }
        return sb.toString();
    }
}
