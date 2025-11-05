package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.l10n.Localization;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Wraps the GrobidService function to be used as a {@link PdfImporter}.
 */
public class PdfGrobidImporter extends BibliographyFromPdfImporter {

    private final GrobidService grobidService;
    private final ImportFormatPreferences importFormatPreferences;

    public PdfGrobidImporter(ImportFormatPreferences importFormatPreferences) {
        this.grobidService = new GrobidService(importFormatPreferences.grobidPreferences());
        this.importFormatPreferences = importFormatPreferences;
    }

    /// Extracts the citation list of the PDF
    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        return new ParserResult(grobidService.processPDF(filePath, importFormatPreferences));
    }

    @Override
    public String getId() {
        return "pdfGrobid";
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
