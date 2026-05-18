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
import org.jabref.model.entry.field.FieldTextMapper;

/// Outputs the findings in a line-oriented `file:line:column: message` format suitable for
/// editors and CI tooling.
///
/// This is the same format produced by
/// [org.jabref.logic.integrity.IntegrityCheckResultErrorFormatWriter]. One line is
/// emitted per deviating field of a reported entry:
///
/// - a field that is absent although other entries of the same type use it, and
/// - an unknown field that is present although other entries of the same type do not use it.
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

    @Override
    protected void writeBibEntry(BibEntry bibEntry, String entryType, Set<Field> requiredFields, Set<Field> optionalFields) throws IOException {
        String citationKey = bibEntry.getCitationKey().orElse("");
        for (Field field : allReportedFields) {
            Optional<String> value = bibEntry.getField(field);
            String fieldName = FieldTextMapper.getDisplayName(field);
            if (value.isPresent()) {
                if (!requiredFields.contains(field) && !optionalFields.contains(field)) {
                    write(parserResult.getFieldRange(bibEntry, field),
                            "%s '%s': unknown field '%s' is present".formatted(entryType, citationKey, fieldName));
                }
            } else {
                write(parserResult.getCompleteEntryIndicator(bibEntry),
                        "%s '%s': field '%s' is absent".formatted(entryType, citationKey, fieldName));
            }
        }
    }

    private void write(ParserResult.Range range, String message) throws IOException {
        writer.append("%s:%d:%d: %s\n".formatted(
                inputFile,
                range.startLine(),
                range.startColumn(),
                message));
    }

    @Override
    public void close() throws IOException {
    }
}
