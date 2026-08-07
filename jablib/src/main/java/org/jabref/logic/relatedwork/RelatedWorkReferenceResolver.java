package org.jabref.logic.relatedwork;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.RuleBasedBibliographyPdfImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class RelatedWorkReferenceResolver {

    private final RuleBasedBibliographyPdfImporter bibliographyPdfImporter;

    public RelatedWorkReferenceResolver() {
        this.bibliographyPdfImporter = new RuleBasedBibliographyPdfImporter();
    }

    /// Parse references from the given PDF file
    ///
    /// @param linkedFile The attached PDF file of the selected bib entry
    /// @return Map from citation key to the corresponding reference entries
    public Map<String, BibEntry> parseReferences(LinkedFile linkedFile,
                                                 BibDatabaseContext databaseContext,
                                                 FilePreferences filePreferences) throws IOException {
        // Get absolute path of PDF file
        Path pdfPath = linkedFile.findIn(databaseContext, filePreferences)
                                 .orElseThrow(() -> new IOException("Failed to resolve linked PDF file: " + linkedFile.getLink() + ". Please check the file and try again."));

        // Locate Reference section and parse references
        PDDocument document = Loader.loadPDF(pdfPath.toFile());
        ParserResult parserResult = bibliographyPdfImporter.importDatabase(pdfPath, document);
        List<BibEntry> parsedEntries = List.copyOf(parserResult.getDatabase().getEntries());

        Map<String, BibEntry> entriesByMarker = new HashMap<>();
        for (BibEntry parsedEntry : parsedEntries) {
            Optional<String> citationMarkerNumber = parsedEntry.getCitationKey();
            citationMarkerNumber.ifPresent(number -> {
                String citationMarker = "[" + number + "]";
                entriesByMarker.putIfAbsent(citationMarker, parsedEntry);
            });
        }

        return entriesByMarker;
    }
}
