package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.jabref.model.FieldChange;
import net.sf.jabref.model.cleanup.CleanupJob;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryConverter;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.lang3.StringUtils;

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
                    changes.add(entry.setField(newFieldName, oldValue).orElse(null));
                    changes.add(entry.clearField(oldFieldName).orElse(null));
                }
            });
        }
        // Dates: create date out of year and month, save it and delete old fields
        // If there already exists a value for the field date, it is not overwritten
        String date = "";
        if (entry.getField(FieldName.DATE).isPresent()) {
            date = entry.getField(FieldName.DATE).get();
        }
        if (StringUtils.isBlank(date)) {
            entry.getFieldOrAlias(FieldName.DATE).ifPresent(newDate -> {
                changes.add(entry.setField(FieldName.DATE, newDate).orElse(null));
                changes.add(entry.clearField(FieldName.YEAR).orElse(null));
                changes.add(entry.clearField(FieldName.MONTH).orElse(null));
            });
        }
        return changes;
    }

}
