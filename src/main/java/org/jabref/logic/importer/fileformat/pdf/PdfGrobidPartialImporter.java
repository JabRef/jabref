package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Wraps the GrobidService function to be used as a {@link PdfPartialImporter}.
 */
public class PdfGrobidPartialImporter extends PdfPartialImporter {

    private final GrobidService grobidService;
    private final ImportFormatPreferences importFormatPreferences;

    public PdfGrobidPartialImporter(ImportFormatPreferences importFormatPreferences) {
        this.grobidService = new GrobidService(importFormatPreferences.grobidPreferences());
        this.importFormatPreferences = importFormatPreferences;
    }

    public List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        return grobidService.processPDF(filePath, importFormatPreferences);
    }

    @Override
    public String getId() {
        return "grobidPdf";
    }

    @Override
    public String getName() {
        return "Grobid";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Imports BibTeX data of a PDF using Grobid.");
    }
}
