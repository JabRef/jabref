package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Wraps the GrobidService function to be used as a {@link PdfBibExtractor}.
 */
public class PdfGrobidBibExtractor implements PdfBibExtractor {

    private final GrobidService grobidService;
    private final ImportFormatPreferences importFormatPreferences;

    public PdfGrobidBibExtractor(ImportFormatPreferences importFormatPreferences) {
        this.grobidService = new GrobidService(importFormatPreferences.grobidPreferences());
        this.importFormatPreferences = importFormatPreferences;
    }

    public List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        return grobidService.processPDF(filePath, importFormatPreferences);
    }
}
