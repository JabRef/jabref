package org.jabref.logic.integrity;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

public class IntegrityCheckResultErrorFormatWriter extends IntegrityCheckResultWriter {

    private final ParserResult parserResult;
    private final Path inputFile;

    public IntegrityCheckResultErrorFormatWriter(Writer writer, List<IntegrityMessage> messages, ParserResult parserResult, Path inputFile) {
        super(writer, messages);
        this.parserResult = parserResult;
        this.inputFile = inputFile;
    }

    // [impl->req~jabkit.cli.check-errorformat-output~1]
    @Override
    public void writeFindings() throws IOException {
        for (IntegrityMessage message : messages) {
            // Entry-level findings (e.g. on the citation key itself) carry only the citation key;
            // field-level findings additionally carry the field name.
            String location = message.entry().getCitationKey().orElse(message.entry().getAuthorTitleYear(5));
            Field field = message.field();
            ParserResult.Range fieldRange = parserResult.getFieldRange(message.entry(), field);
            if (field != InternalField.KEY_FIELD) {
                location += ":" + field.getName();
            }
            writer.append("%s:%d:%d:%s: %s\n".formatted(
                    inputFile,
                    fieldRange.startLine(),
                    fieldRange.startColumn(),
                    location,
                    message.message()));
        }
    }
}
