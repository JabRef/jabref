package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
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
 * The symbols from {@link PaperConsistencyCheckResultWriter} are used.
 */
public class PaperConsistencyCheckResultCsvWriter extends PaperConsistencyCheckResultWriter {

    private CSVPrinter csvPrinter;

    public PaperConsistencyCheckResultCsvWriter(PaperConsistencyCheck.Result result, Writer writer) {
        super(result, writer);
    }

    public PaperConsistencyCheckResultCsvWriter(PaperConsistencyCheck.Result result, Writer writer, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        super(result, writer, entryTypesManager, bibDatabaseMode);
    }

    @Override
    public void writeFindings() throws IOException {
        csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        csvPrinter.printRecord(allFieldNames);
        super.writeFindings();
    }

    @Override
    protected void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException {
        List<String> theRecord = getFindingsAsList(bibEntry, entryType, requiredFields, optionalFields);
        csvPrinter.printRecord(theRecord);
    }

    @Override
    public void close() throws IOException {
        csvPrinter.close();
    }
}
