package org.jabref.logic.anonymization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;

/**
 * This class is used to anonymize a library. It is required to make private libraries available for public use.
 * <p>
 * For "just" generating large .bib files, scripts/bib-file-generator.py can be used.
 */
@NullMarked
public class Anonymization {

    public BibDatabaseContext anonymizeLibrary(BibDatabaseContext bibDatabaseContext) {
        // TODO: Anonymize metadata
        // TODO: Anonymize strings
        List<BibEntry> newEntries = anonymizeEntries(bibDatabaseContext);

        BibDatabase bibDatabase = new BibDatabase(newEntries);

        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());
        return result;
    }

    private static List<BibEntry> anonymizeEntries(BibDatabaseContext bibDatabaseContext) {
        List<BibEntry> entries = bibDatabaseContext.getEntries();
        List<BibEntry> newEntries = new ArrayList<>(entries.size());

        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        for (BibEntry entry : entries) {
            BibEntry newEntry = new BibEntry(entry.getType());
            newEntries.add(newEntry);
            for (Field field : entry.getFields()) {
                Map<String, Integer> valueToIdMap = fieldToValueToIdMap.computeIfAbsent(field, k -> new HashMap<>());
                // TODO: Use {@link org.jabref.model.entry.field.FieldProperty} to distinguish cases.
                //       See {@link org.jabref.model.entry.field.StandardField} for usages.
                String fieldContent = entry.getField(field).get();
                Integer id = valueToIdMap.computeIfAbsent(fieldContent, k -> valueToIdMap.size() + 1);
                newEntry.setField(field, field.getName().toLowerCase(Locale.ROOT) + "-" + id);
            }
        }
        return newEntries;
    }
}
