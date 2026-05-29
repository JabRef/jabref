package org.jabref.logic.ai.ingestion.logic.parsing;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UniversalContentParser implements FileContentParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(UniversalContentParser.class);

    private final PdfContentParser pdfFileParser = new PdfContentParser();

    public Optional<String> parse(Path path) {
        if (FileUtil.isPDFFile(path)) {
            return pdfFileParser.parse(path);
        } else {
            LOGGER.info("Unsupported file type of file: {}. Currently, only PDF files are supported", path);
            return Optional.empty();
        }
    }

    public static boolean isSupportedFileType(Path path) {
        return FileUtil.isPDFFile(path);
    }
}
