package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/// Outputs the findings as CSV.
///
/// The symbols from [BibliographyConsistencyCheckResultWriter] are used.
public class BibliographyConsistencyCheckResultCsvWriter extends BibliographyConsistencyCheckResultWriter {

    private CSVPrinter csvPrinter;

    public BibliographyConsistencyCheckResultCsvWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain) {
        super(result, writer, isPorcelain);
    }

    public BibliographyConsistencyCheckResultCsvWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        super(result, writer, isPorcelain, entryTypesManager, bibDatabaseMode);
    }

    @Override
    public void writeFindings() throws IOException {
        csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        csvPrinter.printRecord(columnNames);
        super.writeFindings();
    }

    @Override
    protected void writeBibEntry(BibEntry bibEntry, EntryTypeReport entryTypeReport) throws IOException {
        List<String> theRecord = getFindingsAsList(bibEntry, entryTypeReport);
        csvPrinter.printRecord(theRecord);
    }

    @Override
    public void close() throws IOException {
        csvPrinter.close();
    }
}
