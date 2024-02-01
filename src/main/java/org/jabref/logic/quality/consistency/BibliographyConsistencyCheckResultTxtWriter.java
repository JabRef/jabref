package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

/**
 * Outputs the findings as plain text.
 * <p>
 * The symbols from {@link BibliographyConsistencyCheckResultWriter} are used.
 */
public class BibliographyConsistencyCheckResultTxtWriter extends BibliographyConsistencyCheckResultWriter {

    private List<Integer> columnWidths;

    public BibliographyConsistencyCheckResultTxtWriter(BibliographyConsistencyCheck.Result result, Writer writer) {
        super(result, writer);
        initializeColumnWidths();
    }

    public BibliographyConsistencyCheckResultTxtWriter(BibliographyConsistencyCheck.Result result, Writer writer, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        super(result, writer, entryTypesManager, bibDatabaseMode);
        initializeColumnWidths();
    }

    public void writeFindings() throws IOException {
        writer.write(Localization.lang("Field Presence Consistency Check Result"));
        writer.write("\n\n");
        writer.write(allFieldNames.stream().collect(Collectors.joining(" | ", "", "\n")));
        writer.write(columnWidths.stream().map(width -> "-".repeat(width)).collect(Collectors.joining(" | ", "| ", " | \n")));
        super.writeFindings();
        writer.write("\n\n");
        writer.write(Localization.lang("Legend"));
        writer.write("\n\n");
        writer.write("%s | %s\n".formatted(REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("required field is present")));
        writer.write("%s | %s\n".formatted(OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("optional field is present")));
        writer.write("%s | %s\n".formatted(UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("unknown field is present")));
        writer.write("%s | %s\n".formatted(UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("field is absent")));
    }

    private void initializeColumnWidths() {
        columnWidths = new ArrayList<>(allFieldNames.size() + 2);

        Integer max = getColumnWidthOfEntryTypes();
        columnWidths.add(max);

        max = getColumnWidthOfCitationKeys(max);
        columnWidths.add(max);

        columnWidths.addAll(allFieldNames.stream().map(String::length).toList());
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
        String output = theRecord.stream()
                                  .map(string -> ("%-" + columnWidths.get(theRecord.indexOf(string)) + "s").formatted(string))
                                  .collect(Collectors.joining(" | ", "| ", " |\n"));
        writer.write(output);
    }

    @Override
    public void close() throws IOException {
    }
}
