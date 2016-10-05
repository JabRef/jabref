package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryConverter;
import net.sf.jabref.model.entry.FieldName;

/**
 * Converts the entry to BibLatex format.
 */
public class BiblatexCleanup implements CleanupJob {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> alias : EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            String oldFieldName = alias.getKey();
            String newFieldName = alias.getValue();
            entry.getField(oldFieldName).ifPresent(oldValue -> {
                if (!oldValue.isEmpty() && (!entry.getField(newFieldName).isPresent())) {
                    // There is content in the old field and no value in the new, so just copy
                    entry.setField(newFieldName, oldValue);
                    changes.add(new FieldChange(entry, newFieldName, null, oldValue));

                    entry.clearField(oldFieldName);
                    changes.add(new FieldChange(entry, oldFieldName, oldValue, null));
                }
            });
        }

        // Dates: create date out of year and month, save it and delete old fields
        entry.getField(FieldName.DATE).ifPresent(date -> {
            if (date.isEmpty()) {
                entry.getFieldOrAlias(FieldName.DATE).ifPresent(newDate -> {
                    Optional<String> oldYear = entry.getField(FieldName.YEAR);
                    Optional<String> oldMonth = entry.getField(FieldName.MONTH);

                    entry.setField(FieldName.DATE, newDate);
                    entry.clearField(FieldName.YEAR);
                    entry.clearField(FieldName.MONTH);

                    changes.add(new FieldChange(entry, FieldName.DATE, null, newDate));
                    changes.add(new FieldChange(entry, FieldName.YEAR, oldYear.orElse(null), null));
                    changes.add(new FieldChange(entry, FieldName.MONTH, oldMonth.orElse(null), null));
                });
            }
        });
        return changes;
    }
}
