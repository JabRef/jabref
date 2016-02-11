package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.CleanupJob;
import net.sf.jabref.model.entry.BibEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Can be used to turn a formatter into a cleanup action.
 */
public class CleanupFormatter implements CleanupJob {

    private Formatter formatter;

    private String fieldKey;

    public CleanupFormatter(Formatter formatter, String fieldKey) {
        this.formatter = formatter;
        this.fieldKey = fieldKey;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> result = new ArrayList<>(0);

        String fieldValue = entry.getField(fieldKey);
        if (fieldValue != null) {
            String newValue = formatter.format(fieldValue);

            //see if something has changed
            if (!fieldValue.equals(newValue)) {
                entry.setField(fieldKey, newValue);
                FieldChange change = new FieldChange(entry, fieldKey, fieldValue, newValue);
                result.add(change);
            }
        }

        return result;
    }
}
