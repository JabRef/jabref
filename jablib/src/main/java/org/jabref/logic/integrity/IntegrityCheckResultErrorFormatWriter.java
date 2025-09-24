package org.jabref.logic.integrity;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;

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
            ParserResult.Range fieldRange = parserResult.getFieldRange(message.entry(), message.field());
            writer.append("%s:%d:%d: %s\n".formatted(
                    inputFile,
                    fieldRange.startLine(),
                    fieldRange.startColumn(),
                    message.message()));
        }
    }
}
