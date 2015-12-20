package net.sf.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Removes a given field.
 */
public class RemoveFieldCleanup implements Cleaner {

    private final String field;


    public RemoveFieldCleanup(String field) {
        this.field = field;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        String oldValue = entry.getField(field);
        if (oldValue == null) {
            // Not set -> nothing to do
            return new ArrayList<>();
        }

        entry.clearField(field);
        FieldChange change = new FieldChange(entry, field, oldValue, null);
        return Arrays.asList(new FieldChange[] {change});
    }

}
