package org.jabref.model.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.event.EntryEventSource;

/**
 * Formats a given entry field with the specified formatter.
 */
public class FieldFormatterCleanup implements CleanupJob {

    private final String field;
    private final Formatter formatter;

    public FieldFormatterCleanup(String field, Formatter formatter) {
        this.field = field;
        this.formatter = formatter;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        if (FieldName.INTERNAL_ALL_FIELD.equalsIgnoreCase(field)) {
            return cleanupAllFields(entry);
        } else if (FieldName.INTERNAL_ALL_TEXT_FIELDS_FIELD.equalsIgnoreCase(field)) {
            return cleanupAllTextFields(entry);
        } else {
            return cleanupSingleField(field, entry);
        }
    }

    /**
     * Runs the formatter on the specified field in the given entry.
     *
     * If the formatter returns an empty string, then the field is removed.
     * @param fieldKey the field on which to run the formatter
     * @param entry the entry to be cleaned up
     * @return a list of changes of the entry
     */
    private List<FieldChange> cleanupSingleField(String fieldKey, BibEntry entry) {
        if (!entry.hasField(fieldKey)) {
            // Not set -> nothing to do
            return new ArrayList<>();
        }
        String oldValue = entry.getField(fieldKey).orElse(null);

        // Run formatter
        String newValue = formatter.format(oldValue);

        if (oldValue.equals(newValue)) {
            return new ArrayList<>();
        } else {
            if (newValue.isEmpty()) {
                entry.clearField(fieldKey);
                newValue = null;
            } else {
                entry.setField(fieldKey, newValue, EntryEventSource.SAVE_ACTION);
            }
            FieldChange change = new FieldChange(entry, fieldKey, oldValue, newValue);
            return Collections.singletonList(change);
        }
    }

    private List<FieldChange> cleanupAllFields(BibEntry entry) {
        List<FieldChange> fieldChanges = new ArrayList<>();

        for (String fieldKey : entry.getFieldNames()) {
            fieldChanges.addAll(cleanupSingleField(fieldKey, entry));
        }

        return fieldChanges;
    }

    private List<FieldChange> cleanupAllTextFields(BibEntry entry) {
        List<FieldChange> fieldChanges = new ArrayList<>();
        Set<String> fields = entry.getFieldNames();
        fields.removeAll(FieldName.getNotTextFieldNames());
        for (String fieldKey : fields) {
            fieldChanges.addAll(cleanupSingleField(fieldKey, entry));
        }

        return fieldChanges;
    }

    public String getField() {
        return field;
    }

    public Formatter getFormatter() {
        return formatter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof FieldFormatterCleanup) {
            FieldFormatterCleanup that = (FieldFormatterCleanup) o;
            return Objects.equals(field, that.field) && Objects.equals(formatter, that.formatter);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, formatter);
    }

    @Override
    public String toString() {
        return field + ": " + formatter.getName();
    }
}
