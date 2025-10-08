package org.jabref.logic.integrity;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.jabref.model.entry.field.FieldTextMapper;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

public class IntegrityCheckResultCsvWriter extends IntegrityCheckResultWriter {

    private CSVPrinter csvPrinter;

    public IntegrityCheckResultCsvWriter(Writer writer, List<IntegrityMessage> messages) {
        super(writer, messages);
    }

    @Override
    public void writeFindings() throws IOException {
        csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        csvPrinter.printRecord("Citation Key", "Field", "Message");
        csvPrinter.printRecords(messages.stream().map(message -> List.of(message.entry().getCitationKey().orElse(""), FieldTextMapper.getDisplayName(message.field()), message.message())));
    }

    @Override
    public void close() throws IOException {
        csvPrinter.close();
    }
}
