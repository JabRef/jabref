package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

/// Outputs the findings in a line-oriented `file:line:column:citationKey:field: message` format
/// suitable for editors and CI tooling.
///
/// This is the same format produced by
/// [org.jabref.logic.integrity.IntegrityCheckResultErrorFormatWriter]. One line is emitted per
/// deviating field of a reported entry:
///
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
    protected void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException {
        String citationKey = bibEntry.getCitationKey().orElse("");
        for (Field field : allReportedFields) {
            Optional<String> value = bibEntry.getField(field);
            if (value.isPresent()) {
                if (!requiredFields.contains(field) && !optionalFields.contains(field)) {
                    write(parserResult.getFieldRange(bibEntry, field), citationKey, field,
                            "unknown field for entry type %s".formatted(entryType));
                }
            } else {
                write(parserResult.getCompleteEntryIndicator(bibEntry), citationKey, field,
                        "field is absent but used by other entries of entry type %s".formatted(entryType));
            }
        }
    }

    private void write(ParserResult.Range range, String citationKey, Field field, String message) throws IOException {
        writer.append("%s:%d:%d:%s:%s: %s\n".formatted(
                inputFile,
                range.startLine(),
                range.startColumn(),
                citationKey,
                field.getName(),
                message));
    }

    @Override
    public void close() throws IOException {
    }
}
