package org.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.util.OptionalUtil;

/**
 * Moves the content of one field to another field.
 */
public class MoveFieldCleanup implements CleanupJob {

    private Field sourceField;
    private Field targetField;

    public MoveFieldCleanup(Field sourceField, Field targetField) {
        this.sourceField = sourceField;
        this.targetField = targetField;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {

        Optional<FieldChange> setFieldChange = entry.getField(sourceField).flatMap(
                value -> entry.setField(targetField, value));
        Optional<FieldChange> clearFieldChange = entry.clearField(sourceField);
        return OptionalUtil.toList(setFieldChange, clearFieldChange);
    }
}
