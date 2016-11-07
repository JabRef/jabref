package net.sf.jabref.logic.cleanup;

import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.util.OptionalUtil;
import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Moves the content of one field to another field.
 */
public class MoveFieldCleanup implements CleanupJob {

    private String sourceField;
    private String targetField;

    public MoveFieldCleanup(String sourceField, String targetField) {
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
