package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Creates the library entry for a PDF that has no sidecar. Metadata is extracted from the PDF
/// itself via [PdfMergeMetadataImporter] (embedded BibTeX, XMP, content heuristics, plus GROBID
/// and identifier lookups as configured — the same pipeline as dropping a PDF onto a library);
/// when nothing usable can be extracted, the entry falls back to a stub titled after the file,
/// so the PDF is at least visible. In both cases the PDF is linked relative to the library root
/// and nothing is written to disk.
@NullMarked
public class PdfEntryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfEntryFactory.class);

    private final PdfMergeMetadataImporter importer;
    private final FilePreferences filePreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    public PdfEntryFactory(ImportFormatPreferences importFormatPreferences,
                           FilePreferences filePreferences,
                           CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.importer = new PdfMergeMetadataImporter(importFormatPreferences);
        this.filePreferences = filePreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
    }

    /// Generates a citation key for the entry if it has none. Call after the entry has been
    /// inserted into the database, so the uniqueness check sees the whole library.
    public void generateCitationKeyIfMissing(BibEntry entry, BibDatabaseContext databaseContext) {
        if (entry.getCitationKey().isPresent()) {
            return;
        }
        new CitationKeyGenerator(databaseContext, citationKeyPatternPreferences).generateAndSetKey(entry);
    }

    public BibEntry createEntry(Path pdf, Path root, BibDatabaseContext databaseContext) {
        BibEntry entry = importPdfMetadata(pdf, databaseContext)
                .orElseGet(() -> new BibEntry(StandardEntryType.Misc));
        if (entry.getField(StandardField.TITLE).isEmpty()) {
            entry.setField(StandardField.TITLE, FileUtil.getBaseName(pdf));
        }
        if (entry.getFiles().isEmpty()) {
            entry.addFile(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName()));
        }
        return entry;
    }

    private Optional<BibEntry> importPdfMetadata(Path pdf, BibDatabaseContext databaseContext) {
        try {
            // The context overload relativizes the file link the importer attaches
            ParserResult parserResult = importer.importDatabase(pdf, databaseContext, filePreferences);
            List<BibEntry> entries = parserResult.getDatabase().getEntries();
            if (parserResult.isInvalid() || entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(entries.getFirst());
        } catch (IOException e) {
            LOGGER.warn("Could not extract metadata from {}", pdf, e);
            return Optional.empty();
        }
    }
}
