package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.logic.importer.plaincitation.ReferencesBlockFromPdfFinder;
import org.jabref.model.entry.BibEntry;

public abstract class PdfImporterWithPlainCitationParser extends PdfImporter implements PlainCitationParser {
    public List<BibEntry> parsePlainCitations(Path pdf) throws FetcherException, IOException {
        return parsePlainCitations(ReferencesBlockFromPdfFinder.getReferencesPagesText(pdf));
    }


}
