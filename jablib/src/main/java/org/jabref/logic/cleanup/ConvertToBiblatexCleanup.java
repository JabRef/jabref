package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Converts the entry to biblatex format.
 */
public class ConvertToBiblatexCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<Field, Field> alias : EntryConverter.FIELD_ALIASES_BIBTEX_TO_BIBLATEX.entrySet()) {
            Field oldField = alias.getKey();
            Field newField = alias.getValue();
            entry.getField(oldField).ifPresent(oldValue -> {
                if (!oldValue.isEmpty() && (entry.getField(newField).isEmpty())) {
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
            // If the year from date field is filled and equal to year it should be removed the year field
            entry.getFieldOrAlias(StandardField.DATE).ifPresent(date -> {
                Optional<Date> newDate = Date.parse(date);
                Optional<Date> checkDate = Date.parse(entry.getFieldOrAlias(StandardField.YEAR),
                        entry.getFieldOrAlias(StandardField.MONTH), Optional.empty());

                if (checkDate.equals(newDate)) {
                    entry.clearField(StandardField.YEAR).ifPresent(changes::add);
                    entry.clearField(StandardField.MONTH).ifPresent(changes::add);
                }
            });
        }
        // If still no 'date' field, try fallback logic
        if (!entry.hasField(StandardField.DATE)) {
            changes.addAll(applyDateFallback(entry));
        }

        return changes;
    }

    private List<FieldChange> applyDateFallback(BibEntry entry) {
        Optional<String> yearValue = entry.getFieldOrAlias(StandardField.YEAR);
        if (yearValue.isEmpty()) {
            return List.of();
        }

        String yearText = yearValue.get();
        if (Date.parse(yearText).isEmpty()) {
            return List.of();
        }

        List<FieldChange> changes = new ArrayList<>();
        // If year was a full date, move it from year to date field.
        entry.setField(StandardField.DATE, yearText).ifPresent(changes::add);
        entry.clearField(StandardField.YEAR).ifPresent(changes::add);

        return changes;
    }
}
