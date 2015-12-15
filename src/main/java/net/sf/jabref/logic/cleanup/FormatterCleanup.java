package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Runs a formatter on every field.
 */
public class FormatterCleanup implements Cleaner {

    private final Formatter formatter;


    public FormatterCleanup(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        ArrayList<FieldChange> changes = new ArrayList<>();
        for (String field : entry.getFieldNames()) {
            String oldValue = entry.getField(field);

            // Run formatter
            String newValue = formatter.format(oldValue);

            if (!oldValue.equals(newValue)) {
                entry.setField(field, newValue);
                FieldChange change = new FieldChange(entry, field, oldValue, newValue);
                changes.add(change);
            }
        }
        return changes;
    }

}
