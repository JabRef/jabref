package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.GitHubActionsEscape;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;

/// Outputs the findings in the
/// [GitHub Actions workflow command format](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#setting-an-error-message)
/// (`::error file=...,line=...,col=...::message`), so that findings show up as annotations on
/// pull requests when `jabkit` runs inside a GitHub Actions workflow.
public class BibliographyConsistencyCheckResultGitHubActionsWriter extends BibliographyConsistencyCheckResultWriter {

    private final ParserResult parserResult;
    private final Path inputFile;

    public BibliographyConsistencyCheckResultGitHubActionsWriter(BibliographyConsistencyCheck.Result result,
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

    // [impl->req~jabkit.cli.check-github-actions-output~1]
    @Override
    protected void writeBibEntry(BibEntry bibEntry, EntryTypeReport entryTypeReport) throws IOException {
        String citationKey = bibEntry.getCitationKey().orElse("");
        String entryType = entryTypeReport.displayName();
        // The citation key and field name go into the message itself (not just the annotation
        // title), so the plain GitHub Actions log line is self-describing.
        String keyPrefix = citationKey.isEmpty() ? "" : citationKey + ": ";
        List<OrFields> missingRequiredFields = entryTypeReport.getMissingRequiredFields(bibEntry);
        for (OrFields missingRequiredField : missingRequiredFields) {
            write(parserResult.getCompleteEntryIndicator(bibEntry), citationKey, missingRequiredField,
                    "%srequired field '%s' is missing".formatted(keyPrefix, FieldFactory.serializeOrFields(missingRequiredField)));
        }
        for (Field field : entryTypeReport.differingFields()) {
            Optional<String> value = bibEntry.getField(field);
            if (value.isPresent()) {
                if (!entryTypeReport.requiredFields().contains(field) && !entryTypeReport.optionalFields().contains(field)) {
                    write(parserResult.getFieldRange(bibEntry, field), citationKey, new OrFields(field),
                            "%sunknown field '%s' for entry type %s".formatted(keyPrefix, field.getName(), entryType));
                }
            } else if (missingRequiredFields.stream().noneMatch(orFields -> orFields.contains(field))) {
                write(parserResult.getCompleteEntryIndicator(bibEntry), citationKey, new OrFields(field),
                        "%sfield '%s' is absent but used by other entries of entry type %s".formatted(keyPrefix, field.getName(), entryType));
            }
        }
    }

    private void write(ParserResult.Range range, String citationKey, OrFields fields, String message) throws IOException {
        String fieldName = FieldFactory.serializeOrFields(fields);
        String title = citationKey.isEmpty() ? fieldName : citationKey + ":" + fieldName;
        writer.append("::error file=%s,line=%d,col=%d,title=%s::%s%n".formatted(
                GitHubActionsEscape.property(inputFile.toString()),
                range.startLine(),
                range.startColumn(),
                GitHubActionsEscape.property(title),
                GitHubActionsEscape.data(message)));
    }

    @Override
    public void close() throws IOException {
    }
}
