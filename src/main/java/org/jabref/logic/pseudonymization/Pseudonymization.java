package org.jabref.logic.pseudonymization;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NullMarked;

/**
 * This class is used to anonymize a library. It is required to make private libraries available for public use.
 * <p>
 * For "just" generating large .bib files, scripts/bib-file-generator.py can be used.
 */
@NullMarked
public class Pseudonymization {

    public record Result(BibDatabaseContext bibDatabaseContext, Map<String, String> valueMapping) {
        public void writeValuesMappingAsCsv(Path path) throws IOException {
            try (
                    OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
                    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)
            ) {
                csvPrinter.printRecord("pseudonymized", "original value");
                valueMapping.entrySet().stream()
                            // We have date-1, date-2, ..., date-10, date-11. That should be sorted accordingly.
                            .sorted(Comparator.comparing((Map.Entry<String, String> entry) -> getKeyPrefix(entry.getKey())
                            ).thenComparingInt(entry -> extractNumber(entry.getKey())))
                            .forEach(Unchecked.consumer(entry -> {
                                csvPrinter.printRecord(entry.getKey(), entry.getValue());
                            }));
            }
        }

        private static String getKeyPrefix(String key) {
            int dashIndex = key.lastIndexOf('-');
            return dashIndex != -1 ? key.substring(0, dashIndex) : key;
        }

        private static int extractNumber(String key) {
            try {
                return Integer.parseInt(key.substring(key.lastIndexOf('-') + 1));
            } catch (NumberFormatException e) {
                return Integer.MAX_VALUE;
            }
        }
    }

    public Result pseudonymizeLibrary(BibDatabaseContext bibDatabaseContext) {
        // TODO: Anonymize metadata
        // TODO: Anonymize strings

        Map<Field, Map<String, Integer>> fieldToValueToIdMap = new HashMap<>();
        List<BibEntry> newEntries = pseudonymizeEntries(bibDatabaseContext, fieldToValueToIdMap);

        Map<String, String> valueMapping = new HashMap<>();
        fieldToValueToIdMap.forEach((field, stringToIntMap) -> {
            stringToIntMap.forEach((value, id) -> {
                valueMapping.put(field.getName().toLowerCase(Locale.ROOT) + "-" + id, value);
            });
        });

        BibDatabase bibDatabase = new BibDatabase(newEntries);
        BibDatabaseContext result = new BibDatabaseContext(bibDatabase);
        result.setMode(bibDatabaseContext.getMode());

        return new Result(result, valueMapping);
    }

    /**
     * @param fieldToValueToIdMap map containing the mapping from field to value to id, will be filled by this method
     */
    private static List<BibEntry> pseudonymizeEntries(BibDatabaseContext bibDatabaseContext, Map<Field, Map<String, Integer>> fieldToValueToIdMap) {
        List<BibEntry> entries = bibDatabaseContext.getEntries();
        List<BibEntry> newEntries = new ArrayList<>(entries.size());

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
