package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.quality.consistency.BibliographyConsistencyCheck.EntryTypeResult;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;

/**
 * Outputs the findings as plain text.
 * <p>
 * The symbols from {@link BibliographyConsistencyCheckResultWriter} are used.
 */
public class BibliographyConsistencyCheckResultTxtWriter extends BibliographyConsistencyCheckResultWriter {

    private List<Integer> columnWidths;

    public BibliographyConsistencyCheckResultTxtWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain) {
        super(result, writer, isPorcelain);
    }

    public BibliographyConsistencyCheckResultTxtWriter(BibliographyConsistencyCheck.Result result, Writer writer, boolean isPorcelain, BibEntryTypesManager entryTypesManager, BibDatabaseMode bibDatabaseMode) {
        super(result, writer, isPorcelain, entryTypesManager, bibDatabaseMode);
    }

    public void writeFindings() throws IOException {
        if (!isPorcelain) {
            writer.write(Localization.lang("Field Presence Consistency Check Result"));
            writer.write("\n\n");
        }

        if (result.entryTypeToResultMap().isEmpty()) {
            if (!isPorcelain) {
                writer.write("No errors found.\n");
            }
            return;
        }

        initializeColumnWidths();

        outputRow(columnNames);

        writer.write(columnWidths.stream().map("-"::repeat).collect(Collectors.joining(" | ", "| ", " |\n")));

        super.writeFindings();

        if (!isPorcelain) {
            int widthSymbol = Localization.lang("Symbol").length();
            int widthMeaning = Collections.max(List.of(
                    Localization.lang("Meaning").length(),
                    Localization.lang("required field is present").length(),
                    Localization.lang("optional field is present").length(),
                    Localization.lang("unknown field is present").length(),
                    Localization.lang("field is absent").length()
            ));

            writer.write("\n");
            writer.write(("| %-" + widthSymbol + "s | %-" + widthMeaning + "s |\n").formatted(Localization.lang("Symbol"), Localization.lang("Meaning")));
            writer.write(("| " + "-".repeat(widthSymbol) + " | " + "-".repeat(widthMeaning) + " |\n").formatted("--", "--"));
            writer.write(("| %-" + widthSymbol + "s | %-" + widthMeaning + "s |\n").formatted(REQUIRED_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("required field is present")));
            writer.write(("| %-" + widthSymbol + "s | %-" + widthMeaning + "s |\n").formatted(OPTIONAL_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("optional field is present")));
            writer.write(("| %-" + widthSymbol + "s | %-" + widthMeaning + "s |\n").formatted(UNKNOWN_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("unknown field is present")));
            writer.write(("| %-" + widthSymbol + "s | %-" + widthMeaning + "s |\n").formatted(UNSET_FIELD_AT_ENTRY_TYPE_CELL_ENTRY, Localization.lang("field is absent")));
        }
    }

    private void initializeColumnWidths() {
        columnWidths = new ArrayList<>(columnNames.size());

        int entryTypeWidth = "entry type".length();
        int citationKeyWidth = "citation key".length();

        for (Map.Entry<EntryType, EntryTypeResult> keysAndValue : result.entryTypeToResultMap().entrySet()) {
            entryTypeWidth = Math.max(entryTypeWidth, keysAndValue.getKey().getDisplayName().length());
            for (BibEntry entry : keysAndValue.getValue().sortedEntries()) {
                citationKeyWidth = Math.max(citationKeyWidth, entry.getCitationKey().orElse("").length());
            }
        }

        columnWidths.add(entryTypeWidth);
        columnWidths.add(citationKeyWidth);
        columnWidths.addAll(columnNames.stream().skip(2).map(String::length).toList());
    }

    @Override
    protected void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException {
        List<String> theRecord = getFindingsAsList(bibEntry, entryType, requiredFields, optionalFields);
        outputRow(theRecord);
    }

    private void outputRow(List<String> theRecord) throws IOException {
        StringJoiner outputJoiner = new StringJoiner(" | ", "| ", " |\n");
        for (int i = 0; i < theRecord.size(); i++) {
            String fieldValue = theRecord.get(i);
            int columnWidth = columnWidths.get(i);
            String formattedField = ("%-" + columnWidth + "s").formatted(fieldValue);
            outputJoiner.add(formattedField);
        }

        writer.write(outputJoiner.toString());
    }

    @Override
    public void close() throws IOException {
    }
}
