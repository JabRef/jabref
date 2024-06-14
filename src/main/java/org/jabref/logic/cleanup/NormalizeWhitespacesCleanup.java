package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.formatter.bibtexfields.NormalizeWhitespaceFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class NormalizeWhitespacesCleanup implements CleanupJob {

    private final NormalizeWhitespaceFormatter formatter;

    public NormalizeWhitespacesCleanup(FieldPreferences fieldPreferences) {
        formatter = new NormalizeWhitespaceFormatter(fieldPreferences);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        List<FieldChange> changes = new ArrayList<>();
        for (Field field : entry.getFields()) {
            // We are sure that the field is set, because this is the assertion of getFields()
            String oldValue = entry.getField(field).get();
            String newValue = formatter.format(oldValue, field);
            if (!newValue.equals(oldValue)) {
                entry.setField(field, newValue).ifPresent(changes::add);
            }
        }
        return changes;
    }
}
