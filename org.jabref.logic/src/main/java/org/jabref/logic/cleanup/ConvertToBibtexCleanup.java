package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jabref.model.FieldChange;
import org.jabref.model.cleanup.CleanupJob;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryConverter;
import org.jabref.model.entry.FieldName;
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
            if (StringUtil.isBlank(entry.getField(FieldName.YEAR))) {
                date.getYear().flatMap(year -> entry.setField(FieldName.YEAR, year.toString())).ifPresent(changes::add);
            }

            if (StringUtil.isBlank(entry.getField(FieldName.MONTH))) {
                date.getMonth().flatMap(month -> entry.setField(FieldName.MONTH, month.getJabRefFormat())).ifPresent(changes::add);
            }

            if (changes.size() > 0) {
                entry.clearField(FieldName.DATE).ifPresent(changes::add);
            }
        });

        for (Map.Entry<String, String> alias : EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            String oldFieldName = alias.getValue();
            String newFieldName = alias.getKey();
            entry.getField(oldFieldName).ifPresent(oldValue -> {
                if (!oldValue.isEmpty() && (!entry.getField(newFieldName).isPresent())) {
                    // There is content in the old field and no value in the new, so just copy
                    entry.setField(newFieldName, oldValue).ifPresent(changes::add);
                    entry.clearField(oldFieldName).ifPresent(changes::add);
                }
            });
        }
        return changes;
    }
}
