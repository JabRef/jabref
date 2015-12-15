package net.sf.jabref.gui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.Cleaner;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryConverter;

/**
 * Converts the entry to BibLatex format.
 */
public class BiblatexCleanup implements Cleaner {

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        ArrayList<FieldChange> changes = new ArrayList<>();
        for (Map.Entry<String, String> alias : EntryConverter.FIELD_ALIASES_TEX_TO_LTX.entrySet()) {
            String oldFieldName = alias.getKey();
            String newFieldName = alias.getValue();
            String oldValue = entry.getField(oldFieldName);
            String newValue = entry.getField(newFieldName);
            if ((oldValue != null) && (!oldValue.isEmpty()) && (newValue == null)) {
                // There is content in the old field and no value in the new, so just copy
                entry.setField(newFieldName, oldValue);
                changes.add(new FieldChange(entry, newFieldName, null, oldValue));

                entry.setField(oldFieldName, null);
                changes.add(new FieldChange(entry, oldFieldName, oldValue, null));
            }
        }

        // Dates: create date out of year and month, save it and delete old fields
        if ((entry.getField("date") == null) || (entry.getField("date").isEmpty())) {
            String newDate = entry.getFieldOrAlias("date");
            String oldYear = entry.getField("year");
            String oldMonth = entry.getField("month");
            entry.setField("date", newDate);
            entry.setField("year", null);
            entry.setField("month", null);

            changes.add(new FieldChange(entry, "date", null, newDate));
            changes.add(new FieldChange(entry, "year", oldYear, null));
            changes.add(new FieldChange(entry, "month", oldMonth, null));
        }
        return changes;
    }
}
