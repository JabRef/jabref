package org.jabref.logic.importer.relatedwork;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * SPI for supplying plain text from a PDF. Implementations may use PDFBox,
 * existing JabRef utilities, or any other backend.
 */
public interface PdfTextProvider {

    /**
     * @param pdf Path to a readable PDF on disk.
     * @return Plain text if extraction succeeds and yields non-empty text; otherwise Optional.empty().
     * @throws IOException for I/O or backend-related failures
     */
    Optional<String> extractPlainText(Path pdf) throws IOException;
}
