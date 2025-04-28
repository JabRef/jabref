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

    private int affectedEntriesCount;

    private final boolean overwriteToFieldContent;

    public MoveFieldValueAction(Field fromField, Field toField, List<BibEntry> entries, NamedCompound edits, boolean overwriteToFieldContent) {
        this.fromField = fromField;
        this.toField = toField;
        this.entries = entries;
        this.edits = edits;
        this.overwriteToFieldContent = overwriteToFieldContent;
    }

    public MoveFieldValueAction(Field fromField, Field toField, List<BibEntry> entries, NamedCompound edits) {
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

        edits.end();
    }

    /**
     * @return the number of affected entries
     * */
    public int executeAndGetAffectedEntriesCount() {
        execute();
        return affectedEntriesCount;
    }
}
