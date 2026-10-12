package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;

/// Outputs the findings in a line-oriented `file:line:column:citationKey:field: message` format
/// suitable for editors and CI tooling.
///
/// This is the same format produced by
/// [org.jabref.logic.integrity.IntegrityCheckResultErrorFormatWriter]. One line is emitted per
/// deviating field of a reported entry:
///
/// - a required field that is absent (alternatives such as `author/editor` are reported as one field),
/// - a field that is absent although other entries of the same type use it, and
/// - an unknown field that is present although other entries of the same type do not use it.
///
/// Both findings are field-level, so every line carries both the citation key and the field name.
public class BibliographyConsistencyCheckResultErrorFormatWriter extends BibliographyConsistencyCheckResultWriter {

    private final ParserResult parserResult;
    private final Path inputFile;

    public BibliographyConsistencyCheckResultErrorFormatWriter(BibliographyConsistencyCheck.Result result,
                                                               Writer writer,
                                                               boolean isPorcelain,
                                                               BibEntryTypesManager entryTypesManager,
                                                               BibDatabaseMode bibDatabaseMode,
                                                               ParserResult parserResult,
                                                               Path inputFile) {
        super(result, writer, isPorcelain, entryTypesManager, bibDatabaseMode);
        this.parserResult = parserResult;
        this.inputFile = inputFile;
    }

    // [impl->req~jabkit.cli.check-errorformat-output~1]
    @Override
    protected void writeBibEntry(BibEntry bibEntry, EntryTypeReport entryTypeReport) throws IOException {
        String citationKey = bibEntry.getCitationKey().orElse("");
        String entryType = entryTypeReport.displayName();
        List<OrFields> missingRequiredFields = entryTypeReport.getMissingRequiredFields(bibEntry);
        for (OrFields missingRequiredField : missingRequiredFields) {
            write(parserResult.getCompleteEntryIndicator(bibEntry), citationKey, missingRequiredField,
                    "required field '%s' is missing".formatted(FieldFactory.serializeOrFields(missingRequiredField)));
        }
        for (Field field : entryTypeReport.differingFields()) {
            Optional<String> value = bibEntry.getField(field);
            if (value.isPresent()) {
                if (!entryTypeReport.requiredFields().contains(field) && !entryTypeReport.optionalFields().contains(field)) {
                    write(parserResult.getFieldRange(bibEntry, field), citationKey, new OrFields(field),
                            "unknown field for entry type %s".formatted(entryType));
                }
            } else if (missingRequiredFields.stream().noneMatch(orFields -> orFields.contains(field))) {
                write(parserResult.getCompleteEntryIndicator(bibEntry), citationKey, new OrFields(field),
                        "field is absent but used by other entries of entry type %s".formatted(entryType));
            }
        }
    }

    private void write(ParserResult.Range range, String citationKey, OrFields fields, String message) throws IOException {
        String fieldName = FieldFactory.serializeOrFields(fields);
        writer.append("%s:%d:%d:%s:%s: %s\n".formatted(
                inputFile,
                range.startLine(),
                range.startColumn(),
                citationKey,
                fieldName,
                message));
    }

    @Override
    public void close() throws IOException {
    }
}
