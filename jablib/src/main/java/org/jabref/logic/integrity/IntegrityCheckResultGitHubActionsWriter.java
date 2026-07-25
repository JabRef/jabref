package org.jabref.logic.integrity;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.GitHubActionsEscape;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

/// Outputs the findings in the
/// [GitHub Actions workflow command format](https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/workflow-commands-for-github-actions#setting-an-error-message)
/// (`::error file=...,line=...,col=...::message`), so that findings show up as annotations on
/// pull requests when `jabkit` runs inside a GitHub Actions workflow.
public class IntegrityCheckResultGitHubActionsWriter extends IntegrityCheckResultWriter {

    private final ParserResult parserResult;
    private final Path inputFile;

    public IntegrityCheckResultGitHubActionsWriter(Writer writer, List<IntegrityMessage> messages, ParserResult parserResult, Path inputFile) {
        super(writer, messages);
        this.parserResult = parserResult;
        this.inputFile = inputFile;
    }

    // [impl->req~jabkit.cli.check-github-actions-output~1]
    @Override
    public void writeFindings() throws IOException {
        for (IntegrityMessage message : messages) {
            String location = message.entry().getCitationKey().orElse(message.entry().getAuthorTitleYear(5));
            Field field = message.field();
            ParserResult.Range fieldRange = parserResult.getFieldRange(message.entry(), field);
            if (field != InternalField.KEY_FIELD) {
                location += ":" + field.getName();
            }
            writer.append("::error file=%s,line=%d,col=%d,title=%s::%s%n".formatted(
                    GitHubActionsEscape.property(inputFile.toString()),
                    fieldRange.startLine(),
                    fieldRange.startColumn(),
                    GitHubActionsEscape.property(location),
                    GitHubActionsEscape.data(message.message())));
        }
    }
}
