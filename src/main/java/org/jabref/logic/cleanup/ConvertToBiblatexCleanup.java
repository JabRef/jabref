package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * Converts the entry to biblatex format.
 */
public class ConvertToBiblatexCleanup implements CleanupJob {

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
            // If the year from date field is filled and equal to year it should be removed the year field
            entry.getFieldOrAlias(StandardField.DATE).ifPresent(date -> {
                entry.getFieldOrAlias(StandardField.YEAR).ifPresent(year -> {
                    entry.getFieldOrAlias(StandardField.MONTH).ifPresent(month -> {

                        Optional<Date> checkDate = Date.parse(date);
                        // Converts using this format: "MMMM, uuuu". Ex: September, 2015
                        month = month.replaceAll("#", "").toLowerCase();
                        month = month.substring(0, 1).toUpperCase() + month.substring(1, month.length());
                        Optional<Date> checkMonthAndYear = Date.parse(month + ", " + year);

                        if (checkDate.isPresent() && checkMonthAndYear.isPresent() && checkDate.equals(checkMonthAndYear)) {
                            entry.clearField(StandardField.YEAR).ifPresent(changes::add);
                            entry.clearField(StandardField.MONTH).ifPresent(changes::add);
                        }
                    });
                });
            });
        }
        return changes;
    }
}
