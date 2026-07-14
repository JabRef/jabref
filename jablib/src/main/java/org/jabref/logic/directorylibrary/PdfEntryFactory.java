package org.jabref.logic.directorylibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
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
    private final CrossRef crossRef;
    private final DoiFetcher doiFetcher;

    public PdfEntryFactory(ImportFormatPreferences importFormatPreferences,
                           FilePreferences filePreferences,
                           CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this(importFormatPreferences, filePreferences, citationKeyPatternPreferences,
                new CrossRef(), new DoiFetcher(importFormatPreferences));
    }

    PdfEntryFactory(ImportFormatPreferences importFormatPreferences,
                    FilePreferences filePreferences,
                    CitationKeyPatternPreferences citationKeyPatternPreferences,
                    CrossRef crossRef,
                    DoiFetcher doiFetcher) {
        this.importer = new PdfMergeMetadataImporter(importFormatPreferences);
        this.filePreferences = filePreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.crossRef = crossRef;
        this.doiFetcher = doiFetcher;
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
        BibEntry entry = extractMetadata(pdf, databaseContext)
                .orElseGet(() -> new BibEntry(StandardEntryType.Misc));
        if (entry.getField(StandardField.TITLE).isEmpty()) {
            entry.setField(StandardField.TITLE, FileUtil.getBaseName(pdf));
        }
        if (entry.getFiles().isEmpty()) {
            entry.addFile(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName()));
        }
        return entry;
    }

    /// The immediately available placeholder for a PDF: title from the file name, PDF linked.
    /// Metadata extraction happens asynchronously afterwards (see [PdfEnrichmentTask]).
    public BibEntry createStub(Path pdf, Path root) {
        BibEntry stub = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, FileUtil.getBaseName(pdf));
        stub.addFile(new LinkedFile("", root.relativize(pdf), StandardFileType.PDF.getName()));
        return stub;
    }

    /// Applies extracted metadata onto the live stub entry (same instance, so table selection
    /// survives). Mutations carry [EntriesEventSource#SHARED]: enrichment is system-initiated,
    /// not a user edit. The stub's file link is preserved.
    public void applyExtractedMetadata(BibEntry extracted, BibEntry stub) {
        if (!stub.getType().equals(extracted.getType())) {
            stub.setType(extracted.getType(), EntriesEventSource.SHARED);
        }
        extracted.getFields().stream()
                 .filter(field -> StandardField.FILE != field)
                 .forEach(field -> extracted.getField(field).ifPresent(
                         value -> stub.setField(field, value, EntriesEventSource.SHARED)));
    }

    public Optional<BibEntry> extractMetadata(Path pdf, BibDatabaseContext databaseContext) {
        try {
            // The context overload relativizes the file link the importer attaches
            ParserResult parserResult = importer.importDatabase(pdf, databaseContext, filePreferences);
            List<BibEntry> entries = parserResult.getDatabase().getEntries();
            if (parserResult.isInvalid() || entries.isEmpty()) {
                return Optional.empty();
            }
            BibEntry entry = entries.getFirst();
            completeWithMissingDoiMetadata(entry);
            return Optional.of(entry);
        } catch (IOException e) {
            LOGGER.warn("Could not extract metadata from {}", pdf, e);
            return Optional.empty();
        }
    }

    /// When the PDF itself yielded no DOI, the DOI is looked up (the same reverse search as the
    /// magnifier button next to the DOI field) and the metadata behind it fills the fields the
    /// PDF did not provide — extracted values are never overwritten. PDFs that already carried a
    /// DOI were enriched by [PdfMergeMetadataImporter] itself.
    private void completeWithMissingDoiMetadata(BibEntry entry) {
        if (entry.getField(StandardField.DOI).isPresent()) {
            return;
        }
        try {
            Optional<DOI> doi = crossRef.findIdentifier(entry);
            if (doi.isEmpty()) {
                return;
            }
            entry.setField(StandardField.DOI, doi.get().asString());
            doiFetcher.performSearchById(doi.get().asString()).ifPresent(entry::mergeWith);
        } catch (FetcherException e) {
            LOGGER.debug("DOI lookup failed for {}", entry.getAuthorTitleYear(80), e);
        }
    }
}
