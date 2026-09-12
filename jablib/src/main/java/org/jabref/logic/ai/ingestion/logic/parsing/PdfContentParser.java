package org.jabref.logic.ai.ingestion.logic.parsing;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.pdf.InterruptablePDFTextStripper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->req~ai.ingestion.pdf-handling~1]
@NullMarked
public class PdfContentParser implements FileContentParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfContentParser.class);

    @Override
    public List<String> parse(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            int numberOfPages = document.getNumberOfPages();
            List<String> pages = new ArrayList<>(numberOfPages);

            InterruptablePDFTextStripper stripper = new InterruptablePDFTextStripper();
            for (int page = 1; page <= numberOfPages; page++) {
                if (Thread.currentThread().isInterrupted()) {
                    return List.of();
                }
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                StringWriter writer = new StringWriter();
                stripper.writeText(document, writer);
                pages.add(writer.toString());
            }

            return pages;
        } catch (IOException e) {
            LOGGER.error("An error occurred while reading the PDF file: {}", path, e);
            return List.of();
        }
    }
}
