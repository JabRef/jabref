package org.jabref.logic.anonymization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;

/**
 * This class is used to anonymize a library. It is required to make private libraries available for public use.
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

        Map<Field, Map<String, Integer>> fieldToValueToCountMap = new HashMap<>();
        int currentCount = 0;
        for (BibEntry entry : entries) {
            BibEntry newEntry = new BibEntry(entry.getType());
            newEntries.add(newEntry);
            for (Field field : entry.getFields()) {
                if (!fieldToValueToCountMap.containsKey(field)) {
                    fieldToValueToCountMap.put(field, new HashMap<>());
                }

                String value = entry.getField(field).get();
                if (!fieldToValueToCountMap.get(field).containsKey(value)) {
                    // Current anonymization is using a simple counter.
                    // TODO: Use {@link org.jabref.model.entry.field.FieldProperty} to distinguish cases.
                    //       See {@link org.jabref.model.entry.field.StandardField} for usages.
                    fieldToValueToCountMap.get(field).put(value, currentCount++);
                }

                Integer count = fieldToValueToCountMap.get(field).get(value);
                fieldToValueToCountMap.get(field).put(value, count);

                newEntry.setField(field, count.toString());
            }
        }
        return newEntries;
    }
}
