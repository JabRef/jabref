package org.jabref.logic.ai.ingestion;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilReader;

import dev.langchain4j.data.document.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileToDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileToDocument.class);

    public static Optional<Document> fromFile(Path path) {
        if (FileUtil.isPDFFile(path)) {
            return FileToDocument.fromPdfFile(path);
        } else {
            LOGGER.info("Unsupported file type of file: {}. Currently, only PDF files are supported", path);
            return Optional.empty();
        }
    }

    private static Optional<Document> fromPdfFile(Path path) {
        // This method is private to ensure that the path is really pointing to PDF file (determined by extension).

        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(path)) {
            int lastPage = document.getNumberOfPages();
            StringWriter writer = new StringWriter();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(lastPage);
            stripper.writeText(document, writer);

            return FileToDocument.fromString(writer.toString());
        } catch (Exception e) {
            LOGGER.error("An error occurred while reading the PDF file: {}", path, e);
            return Optional.empty();
        }
    }

    public static Optional<Document> fromString(String content) {
        return Optional.of(new Document(content));
    }
}
