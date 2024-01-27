package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

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
public class PaperConsistencyCheckResultCsvWriter extends PaperConsistencyCheckResultWriter {

    private static final String REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "x";
    private static final String OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "o";
    private static final String UNKOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY = "?";

    private OutputStreamWriter writer;
    private CSVPrinter csvPrinter;
    private int columnCount;

    public PaperConsistencyCheckResultCsvWriter(PaperConsistencyCheck.Result result, Path path) {
        super(result, path);
    }

    public PaperConsistencyCheckResultCsvWriter(PaperConsistencyCheck.Result result, Path path, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        super(result, path, entryTypesManager, bibDatabaseMode);
    }

    public void writeFindings() throws IOException {
        writer = new OutputStreamWriter(Files.newOutputStream(path), StandardCharsets.UTF_8);
        csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

        List<String> theHeading = new ArrayList(columnCount);
        theHeading.add("entry type");
        theHeading.add("citation key");
        allFields.forEach(field -> {
            theHeading.add(field.getDisplayName());
        });
        csvPrinter.printRecord(theHeading);
        columnCount = allFields.size() + 2;

        super.writeFindings();
    }

    @Override
    protected void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException {
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

    @Override
    public void close() throws IOException {
        csvPrinter.close();
        writer.close();
    }
}
