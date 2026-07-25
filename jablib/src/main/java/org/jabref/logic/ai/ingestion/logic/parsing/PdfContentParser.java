package org.jabref.logic.ai.ingestion.logic.parsing;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.pdf.InterruptablePDFTextStripper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->req~ai.ingestion.pdf-handling~1]
public class PdfContentParser implements FileContentParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfContentParser.class);

    @Override
    public Optional<String> parse(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            int lastPage = document.getNumberOfPages();
            StringWriter writer = new StringWriter();

            InterruptablePDFTextStripper stripper = new InterruptablePDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(lastPage);
            stripper.writeText(document, writer);

            return Optional.of(writer.toString());
        } catch (IOException e) {
            LOGGER.error("An error occurred while reading the PDF file: {}", path, e);
            return Optional.empty();
        }
    }
}
