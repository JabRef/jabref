package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jooq.lambda.Unchecked;

public class PaperConsistencyCheckResultCsvWriter {

    private static final String REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "x";
    private static final String OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "o";
    private static final String UNKOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "?";

    public static void writeFindingsAsCsv(PaperConsistencyCheck.Result result, Path path) throws IOException {
        writeFindingsAsCsv(result, path, new BibEntryTypesManager(), BibDatabaseMode.BIBTEX);
    }

    /**
     * Outputs the findings as CSV.
     * <p>
     * Following symbols are used:
     *
     * <ul>
     *     <li><code>x</code> - required field is present</li>
     *     <li><code>o</code> - optional field is present</li>
     *     <li><code>?</code> - unknown field is present</li>
     * </ul>
     * <p>
     * Note that this classification is based on JabRef's definition and might not match the publisher's definition.
     *
     * @implNote We could have implemented a <code>PaperConsistencyCheckResultFormatter</code>, but that would have been too much effort.
     */
    public static void writeFindingsAsCsv(PaperConsistencyCheck.Result result, Path path, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) throws IOException {
        try (
                OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)
        ) {
            List<Field> allFields = result.entryTypeToResultMap().values().stream()
                                          .flatMap(entryTypeResult -> entryTypeResult.fields().stream())
                                          .sorted(Comparator.comparing(Field::getName))
                                          .distinct()
                                          .toList();
            int columnCount = allFields.size() + 2;

            // heading
            List<String> theHeading = new ArrayList(columnCount);
            theHeading.add("entry type");
            theHeading.add("citation key");
            allFields.forEach(field -> {
                theHeading.add(field.getDisplayName());
            });
            csvPrinter.printRecord(theHeading);

            // content
            result.entryTypeToResultMap().entrySet().stream()
                  .sorted(Comparator.comparing(entry -> entry.getKey().getName()))
                  .forEach(Unchecked.consumer(mapEntry -> {
                      writeMapEntryToCsv(csvPrinter, mapEntry, columnCount, allFields, entryTypesManager, bibDatabaseMode);
                  }));
        }
    }

    private static void writeMapEntryToCsv(CSVPrinter csvPrinter, Map.Entry<EntryType, PaperConsistencyCheck.EntryTypeResult> mapEntry, int columnCount, List<Field> allFields, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        String entryType = mapEntry.getKey().getDisplayName();

        Optional<BibEntryType> bibEntryType = entryTypesManager.enrich(mapEntry.getKey(), bibDatabaseMode);
        Set<Field> requiredFields = bibEntryType
                .map(BibEntryType::getRequiredFields)
                .stream()
                .flatMap(orFieldsCollection -> orFieldsCollection.stream())
                .flatMap(orFields -> orFields.getFields().stream())
                .collect(Collectors.toSet());
        Set<Field> optionalFields = bibEntryType
                .map(BibEntryType::getOptionalFields)
                .stream()
                .flatMap(bibFieldSet -> bibFieldSet.stream())
                .map(BibField::field)
                .collect(Collectors.toSet());

        PaperConsistencyCheck.EntryTypeResult entries = mapEntry.getValue();
        SequencedCollection<BibEntry> bibEntries = entries.sortedEntries();

        bibEntries.forEach(Unchecked.consumer(bibEntry -> {
            writeBibEntry(csvPrinter, bibEntry, columnCount, allFields, entryType, requiredFields, optionalFields);
        }));
    }

    private static void writeBibEntry(CSVPrinter csvPrinter, BibEntry bibEntry, int columnCount, List<Field> allFields, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException {
        List<String> theRecord = new ArrayList(columnCount);
        theRecord.add(entryType);
        theRecord.add(bibEntry.getCitationKey().orElse(""));
        allFields.forEach(field -> {
            theRecord.add(bibEntry.getField(field).map(value -> {
                if (requiredFields.contains(field)) {
                    return REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY;
                } else if (optionalFields.contains(field)) {
                    return OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY;
                } else {
                    return UNKOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY;
                }
            }).orElse("-"));
        });
        csvPrinter.printRecord(theRecord);
    }
}
