package org.jabref.logic.importer.fileformat.pdf;

import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.logic.importer.plaincitation.ReferencesBlockFromPdfFinder;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;

public abstract class PdfImporterWithPlainCitationParser extends BibliographyFromPdfImporter implements PlainCitationParser {

    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) {
        try {
            String pagesText = ReferencesBlockFromPdfFinder.getReferencesPagesText(document);
            List<BibEntry> entries = parseMultiplePlainCitations(pagesText);
            return new ParserResult(entries);
        } catch (Exception e) {
            return ParserResult.fromError(e);
        }
    }
}
