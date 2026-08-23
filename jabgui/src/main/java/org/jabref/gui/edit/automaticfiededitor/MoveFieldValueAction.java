package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableFieldChange;

public class MoveFieldValueAction extends SimpleCommand {
    private final Field fromField;
    private final Field toField;
    private final List<BibEntry> entries;

    private final CompoundEdit edits;

    private int affectedEntriesCount;

    private final boolean overwriteToFieldContent;

    public MoveFieldValueAction(Field fromField, Field toField, List<BibEntry> entries, CompoundEdit edits, boolean overwriteToFieldContent) {
        this.fromField = fromField;
        this.toField = toField;
        this.entries = entries;
        this.edits = edits;
        this.overwriteToFieldContent = overwriteToFieldContent;
    }

    public MoveFieldValueAction(Field fromField, Field toField, List<BibEntry> entries, CompoundEdit edits) {
        this(fromField, toField, entries, edits, true);
    }

    @Override
    public void execute() {
        affectedEntriesCount = 0;
        for (BibEntry entry : entries) {
            String fromFieldValue = entry.getField(fromField).orElse("");
            String toFieldValue = entry.getField(toField).orElse("");
            if (StringUtil.isNotBlank(fromFieldValue)) {
                if (overwriteToFieldContent || toFieldValue.isEmpty()) {
                    entry.setField(toField, fromFieldValue);
                    entry.setField(fromField, "");

                    edits.addEdit(new UndoableFieldChange(entry, fromField, fromFieldValue, null));
                    edits.addEdit(new UndoableFieldChange(entry, toField, toFieldValue, fromFieldValue));
                    affectedEntriesCount++;
                }
            }
        }
    }

    /// @return the number of affected entries
    public int executeAndGetAffectedEntriesCount() {
        execute();
        return affectedEntriesCount;
    }
}
