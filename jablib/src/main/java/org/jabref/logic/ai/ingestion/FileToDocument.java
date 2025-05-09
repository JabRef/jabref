package org.jabref.logic.ai.ingestion;

import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.pdf.InterruptablePDFTextStripper;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpUtilReader;

import dev.langchain4j.data.document.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileToDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileToDocument.class);

    private final ReadOnlyBooleanProperty shutdownSignal;

    public FileToDocument(ReadOnlyBooleanProperty shutdownSignal) {
        this.shutdownSignal = shutdownSignal;
    }

    public Optional<Document> fromFile(Path path) {
        if (FileUtil.isPDFFile(path)) {
            return fromPdfFile(path);
        } else {
            LOGGER.info("Unsupported file type of file: {}. Currently, only PDF files are supported", path);
            return Optional.empty();
        }
    }

    private Optional<Document> fromPdfFile(Path path) {
        // This method is private to ensure that the path is really pointing to PDF file (determined by extension).

        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(path)) {
            int lastPage = document.getNumberOfPages();
            StringWriter writer = new StringWriter();

            InterruptablePDFTextStripper stripper = new InterruptablePDFTextStripper(shutdownSignal);
            stripper.setStartPage(1);
            stripper.setEndPage(lastPage);
            stripper.writeText(document, writer);

            if (shutdownSignal.get()) {
                return Optional.empty();
            }

            return fromString(writer.toString());
        } catch (Exception e) {
            LOGGER.error("An error occurred while reading the PDF file: {}", path, e);
            return Optional.empty();
        }
    }

    public Optional<Document> fromString(String content) {
        return Optional.of(new Document(content));
    }
}
