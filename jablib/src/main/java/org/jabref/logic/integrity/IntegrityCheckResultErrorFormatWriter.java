package org.jabref.logic.integrity;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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

    @Override
    public void writeFindings() throws IOException {
        for (IntegrityMessage message : messages) {
            Map<Field, ParserResult.Range> fieldRangeMap = parserResult.getFieldRanges().getOrDefault(message.entry(), Map.of());
            ParserResult.Range fieldRange = fieldRangeMap.getOrDefault(message.field(), fieldRangeMap.getOrDefault(InternalField.KEY_FIELD, parserResult.getArticleRanges().getOrDefault(message.entry(), ParserResult.Range.NULL_RANGE)));

            writer.append("%s:%d:%d: %s\n".formatted(
                    inputFile,
                    fieldRange.startLine(),
                    fieldRange.startColumn(),
                    message.message()));
        }
    }
}
