package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

/**
 * Outputs the findings as plain text.
 * <p>
 * The symbols from {@link PaperConsistencyCheckResultWriter} are used.
 */
public class PaperConsistencyCheckResultTxtWriter extends PaperConsistencyCheckResultWriter {

    private List<Integer> columnWidths;

    public PaperConsistencyCheckResultTxtWriter(PaperConsistencyCheck.Result result, Writer writer) {
        super(result, writer);
    }

    public PaperConsistencyCheckResultTxtWriter(PaperConsistencyCheck.Result result, Writer writer, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        super(result, writer, entryTypesManager, bibDatabaseMode);
    }

    public void writeFindings() throws IOException {
        writer.write("Paper Consistency Check Result\n\n");
        if (result.entryTypeToResultMap().isEmpty()) {
            writer.write("No errors found.\n");
            return;
        }

        initializeColumnWidths();

        StringJoiner headerJoiner = new StringJoiner(" | ", "| ", " |\n");
        for (int i = 0; i < columnNames.size(); i++) {
            String fieldName = columnNames.get(i);
            int columnWidth = columnWidths.get(i);
            String formattedField = String.format("%-" + columnWidth + "s", fieldName);
            headerJoiner.add(formattedField);
        }
        writer.write(headerJoiner.toString());

        writer.write(columnWidths.stream().map(width -> "-".repeat(width)).collect(Collectors.joining(" | ", "| ", " |\n")));

        super.writeFindings();
    }

    private void initializeColumnWidths() {
        columnWidths = new ArrayList<>(columnNames.size());

        Integer max = getColumnWidthOfEntryTypes();
        columnWidths.add(max);

        max = getColumnWidthOfCitationKeys(max);
        columnWidths.add(max);

        columnWidths.addAll(columnNames.stream().skip(2).map(String::length).toList());
    }

    private Integer getColumnWidthOfEntryTypes() {
        Integer max = result.entryTypeToResultMap().keySet()
                            .stream()
                            .map(entryType -> entryType.getDisplayName().length())
                            .max(Integer::compareTo)
                            .get();
        max = Math.max(max, "entry type".length());
        return max;
    }

    private Integer getColumnWidthOfCitationKeys(Integer max) {
        result.entryTypeToResultMap().values()
              .stream()
              .flatMap(entryTypeResult -> entryTypeResult.sortedEntries().stream())
              .map(entry -> entry.getCitationKey().orElse("").length())
              .max(Integer::compareTo)
              .get();
        return Math.max(max, "citation key".length());
    }

    @Override
    protected void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException {
        List<String> theRecord = getFindingsAsList(bibEntry, entryType, requiredFields, optionalFields);

        StringJoiner outputJoiner = new StringJoiner(" | ", "| ", " |\n");
        for (int i = 0; i < theRecord.size(); i++) {
            String fieldValue = theRecord.get(i);
            int columnWidth = columnWidths.get(i);
            String formattedField = String.format("%-" + columnWidth + "s", fieldValue);
            outputJoiner.add(formattedField);
        }

        writer.write(outputJoiner.toString());
    }

    @Override
    public void close() throws IOException {
    }
}
