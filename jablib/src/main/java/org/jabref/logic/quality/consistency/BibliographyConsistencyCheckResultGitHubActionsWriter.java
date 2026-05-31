package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.GitHubActionsEscape;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;

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
        String title = citationKey.isEmpty() ? field.getName() : citationKey + ":" + field.getName();
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
