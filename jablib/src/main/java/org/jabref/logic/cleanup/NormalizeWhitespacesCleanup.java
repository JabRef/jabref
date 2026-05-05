package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.formatter.bibtexfields.NormalizeWhitespaceFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

public class NormalizeWhitespacesCleanup implements CleanupJob {

    private static final Collection<Field> NO_TEXT_FIELDS = FieldFactory.getNotTextFields();

    private final NormalizeWhitespaceFormatter formatter;

    public NormalizeWhitespacesCleanup(FieldPreferences fieldPreferences) {
        formatter = new NormalizeWhitespaceFormatter(fieldPreferences);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        return cleanup(entry, Runnable::run);
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry, Consumer<Runnable> mutationScheduler) {
        List<FieldChange> changes = new ArrayList<>();
        for (Field field : entry.getFields()) {
            if (NO_TEXT_FIELDS.contains(field)) {
                continue;
            }
            // We are sure that the field is set, because this is the assertion of getFields()
            // Computation runs on the calling (background) thread:
            String oldValue = entry.getField(field).orElseThrow();
            String newValue = formatter.format(oldValue, field);
            if (!newValue.equals(oldValue)) {
                // Only the actual field mutation is dispatched via the scheduler:
                mutationScheduler.accept(() -> entry.setField(field, newValue).ifPresent(changes::add));
            }
        }
        return changes;
    }
}
