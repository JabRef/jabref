package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * Converts the entry to biblatex format.
 */
public class ConvertToBiblatexCleanup implements CleanupJob {

    @SuppressWarnings("checkstyle:WhitespaceAround")
    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<Field, Field> alias : EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            Field oldField = alias.getKey();
            Field newField = alias.getValue();
            entry.getField(oldField).ifPresent(oldValue -> {
                if (!oldValue.isEmpty() && (!entry.getField(newField).isPresent())) {
                    // There is content in the old field and no value in the new, so just copy
                    entry.setField(newField, oldValue).ifPresent(changes::add);
                    entry.clearField(oldField).ifPresent(changes::add);
                }
            });
        }
        // Dates: create date out of year and month, save it and delete old fields
        // If there already exists a non blank/empty value for the field date, it is not overwritten
        if (StringUtil.isBlank(entry.getField(StandardField.DATE))) {
            entry.getFieldOrAlias(StandardField.DATE).ifPresent(newDate -> {
                entry.setField(StandardField.DATE, newDate).ifPresent(changes::add);
                entry.clearField(StandardField.YEAR).ifPresent(changes::add);
                entry.clearField(StandardField.MONTH).ifPresent(changes::add);
            });
        } else {
            // If date field is filled and is equal to year it should be removed the year and month fields
            entry.getFieldOrAlias(StandardField.DATE).ifPresent(date -> {
                entry.getFieldOrAlias(StandardField.YEAR).ifPresent(yearl -> {
                    // Get the year of the date field (4 digits).
                    if (date.substring(0, 4).equals(yearl)) {
                        entry.clearField(StandardField.YEAR).ifPresent(changes::add);
                        entry.clearField(StandardField.MONTH).ifPresent(changes::add);
                    }
                });
            });
        }
        return changes;
    }
}
