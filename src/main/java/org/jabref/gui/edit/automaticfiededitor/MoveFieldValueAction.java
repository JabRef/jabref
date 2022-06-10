package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

public class MoveFieldValueAction extends SimpleCommand {
    private final Field fromField;
    private final Field toField;
    private final List<BibEntry> entries;

    private final NamedCompound edits;

    public MoveFieldValueAction(Field fromField, Field toField, List<BibEntry> entries, NamedCompound edits) {
        this.fromField = fromField;
        this.toField = toField;
        this.entries = entries;
        this.edits = edits;
    }

    @Override
    public void execute() {
        for (BibEntry entry : entries) {
            String fromFieldValue = entry.getField(fromField).orElse("");
            String toFieldValue = entry.getField(toField).orElse("");

            if (StringUtil.isNotBlank(fromFieldValue)) {
                entry.setField(toField, fromFieldValue);
                entry.setField(fromField, "");

                edits.addEdit(new UndoableFieldChange(entry, fromField, fromFieldValue, null));
                edits.addEdit(new UndoableFieldChange(entry, toField, toFieldValue, fromFieldValue));
            }
        }
        edits.end();
    }
}
