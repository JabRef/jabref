package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * Converts the entry to biblatex format.
 */
public class ConvertToBibtexCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();

        // Dates: get date and fill year and month
        // If there already exists a non blank/empty value for the field, then it is not overwritten
        entry.getPublicationDate().ifPresent(date -> {
            if (StringUtil.isBlank(entry.getField(StandardField.YEAR))) {
                date.getYear().flatMap(year -> entry.setField(StandardField.YEAR, year.toString())).ifPresent(changes::add);
            }

            if (StringUtil.isBlank(entry.getField(StandardField.MONTH))) {
                date.getMonth().flatMap(month -> entry.setField(StandardField.MONTH, month.getJabRefFormat())).ifPresent(changes::add);
            }

            if (changes.size() > 0) {
                entry.clearField(StandardField.DATE).ifPresent(changes::add);
            }
        });

        for (Map.Entry<Field, Field> alias : EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            Field oldField = alias.getValue();
            Field newField = alias.getKey();
            entry.getField(oldField).ifPresent(oldValue -> {
                if (!oldValue.isEmpty() && (!entry.getField(newField).isPresent())) {
                    // There is content in the old field and no value in the new, so just copy
                    entry.setField(newField, oldValue).ifPresent(changes::add);
                    entry.clearField(oldField).ifPresent(changes::add);
                }
            });
        }
        return changes;
    }
}
