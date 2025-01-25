package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibtexImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries to import BibTeX data trying multiple {@link PdfImporter}s and merging the results.
 * See {@link PdfMergeMetadataImporter#metadataImporters} for the list of importers used.
 * <p>
 * After all importers are applied, this importer tries to fetch additional metadata for the entry using the DOI and ISBN.
 */
public class PdfMergeMetadataImporter extends PdfImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMergeMetadataImporter.class);

    private final ImportFormatPreferences importFormatPreferences;
    private final List<PdfImporter> metadataImporters;

    public PdfMergeMetadataImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;

        // TODO: Evaluate priorities of these {@link PdfBibExtractor}s.
        this.metadataImporters = new ArrayList<>(List.of(
                new PdfVerbatimBibtexImporter(importFormatPreferences),
                new PdfEmbeddedBibFileImporter(importFormatPreferences),
                new PdfXmpImporter(importFormatPreferences.xmpPreferences()),
                new PdfContentImporter()
        ));

        if (importFormatPreferences.grobidPreferences().isGrobidEnabled()) {
            this.metadataImporters.add(2, new PdfGrobidImporter(importFormatPreferences));
        }
    }

    /**
     * Makes {@link BibEntry} out of PDF file via merging results of several PDF analysis steps ({@link PdfImporter}).
     * <p>
     * Algorithm:
     * 1. Store all candidates (possible {@link BibEntry}ies) in a list. First elements in this list will have higher
     * priority for merging, which means that more fields will be stored for first entries, rather than last.
     * 2. Run {@link PdfImporter}s, and store extracted candidates in the list.
     */
    @Override
    public List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        List<BibEntry> extractedCandidates = extractCandidatesFromPdf(filePath, document);
        if (extractedCandidates.isEmpty()) {
            return List.of();
        }

        List<BibEntry> fetchedCandidates = fetchIdsOfCandidates(extractedCandidates);

        Stream<BibEntry> allCandidates = Stream.concat(fetchedCandidates.stream(), extractedCandidates.stream());
        BibEntry entry = mergeCandidates(allCandidates);

        // We use the absolute path here as we do not know the context where this import will be used.
        // The caller is responsible for making the path relative if necessary.
        entry.addFile(new LinkedFile("", filePath, StandardFileType.PDF.getName()));
        return List.of(entry);
    }

    private List<BibEntry> extractCandidatesFromPdf(Path filePath, PDDocument document) {
        List<BibEntry> candidates = new ArrayList<>();

        for (PdfImporter metadataImporter : metadataImporters) {
            try {
                List<BibEntry> extractedEntries = metadataImporter.importDatabase(filePath, document);
                candidates.addAll(extractedEntries);
            } catch (Exception e) {
                LOGGER.error("Got an exception while importing PDF file", e);
            }
        }

        return candidates;
    }

    private List<BibEntry> fetchIdsOfCandidates(List<BibEntry> candidates) {
        List<BibEntry> fetchedCandidates = new ArrayList<>();

        for (BibEntry candidate : candidates) {
            Optional<String> doi = candidate.getField(StandardField.DOI);
            if (doi.isPresent()) {
                try {
                    new DoiFetcher(importFormatPreferences)
                            .performSearchById(doi.get())
                            .ifPresent(fetchedCandidates::add);
                } catch (FetcherException e) {
                    LOGGER.error("Fetching failed for DOI \"{}\".", doi, e);
                }
            }

            Optional<String> eprint = candidate.getField(StandardField.EPRINT);
            if (eprint.isPresent()) {
                // It's not exactly right, arXiv is not the only preprint service.
                // There should be check for `archivePrefix = {arXiv}`, but I'm worried if user didn't set it.
                try {
                    new ArXivFetcher(importFormatPreferences)
                            .performSearchById(eprint.get())
                            .ifPresent(fetchedCandidates::add);
                } catch (FetcherException e) {
                    LOGGER.error("Fetching failed for eprint \"{}\".", eprint.get(), e);
                }
            }

            Optional<String> isbn = candidate.getField(StandardField.ISBN);
            if (isbn.isPresent()) {
                try {
                    new IsbnFetcher(importFormatPreferences)
                            // .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences))
                            // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences))
                            .performSearchById(isbn.get())
                            .ifPresent(fetchedCandidates::add);
                } catch (FetcherException e) {
                    LOGGER.error("Fetching failed for ISBN \"{}\".", isbn.get(), e);
                }
            }
            if (candidate.hasField(StandardField.EPRINT)) {
                try {
                    new ArXivFetcher(importFormatPreferences).performSearchById(candidate.getField(StandardField.EPRINT).get()).ifPresent(fetchedCandidates::add);
                } catch (FetcherException e) {
                    LOGGER.error("Fetching failed for arXiv ID \"{}\".", candidate.getField(StandardField.EPRINT).get(), e);
                }
            }

            // TODO: Handle URLs too.
            // However, it may have problems if URL refers to the same identifier in DOI, ISBN, or arXiv.
        }

        return fetchedCandidates;
    }

    private static BibEntry mergeCandidates(Stream<BibEntry> candidates) {
        BibEntry entry = new BibEntry();

        // Functional style is used here (instead of imperative like in `extractCandidatesFromPdf` or `fetchIdsOfCandidates`,
        // because they have checked exceptions.

        candidates.forEach(candidate -> {
            if (BibEntry.DEFAULT_TYPE.equals(entry.getType())) {
                entry.setType(candidate.getType());
            }

            Set<Field> presentFields = entry.getFields();

            candidate
                    .getFieldMap()
                    .entrySet()
                    .stream()
                    // Don't merge FILE fields that point to a stored file as we set that to filePath anyway.
                    // Nevertheless, retain online links.
                    .filter(fieldEntry ->
                            !(StandardField.FILE == fieldEntry.getKey()
                                    && FileFieldParser.parse(fieldEntry.getValue()).stream().noneMatch(LinkedFile::isOnlineLink)))
                    // Only overwrite non-present fields
                    .filter(fieldEntry -> !presentFields.contains(fieldEntry.getKey()))
                    .forEach(fieldEntry -> entry.setField(fieldEntry.getKey(), fieldEntry.getValue()));
        });

        return entry;
    }

    public ParserResult importDatabase(Path filePath, BibDatabaseContext context, FilePreferences filePreferences) throws IOException {
        Objects.requireNonNull(context);
        Objects.requireNonNull(filePreferences);

        return importDatabase(filePath);
    }

    @Override
    public String getId() {
        return "pdfMerged";
    }

    @Override
    public String getName() {
        return Localization.lang("PDF meta data merger");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Imports BibTeX data from a PDF using multiple strategies (e.g., XMP, embedded BibTeX, text parsing, Grobid, and DOI lookup) and merges the result.");
    }

    public static class EntryBasedFetcherWrapper extends PdfMergeMetadataImporter implements EntryBasedFetcher {

        private final FilePreferences filePreferences;
        private final BibDatabaseContext databaseContext;

        public EntryBasedFetcherWrapper(ImportFormatPreferences importFormatPreferences, FilePreferences filePreferences, BibDatabaseContext context) {
            super(importFormatPreferences);
            this.filePreferences = filePreferences;
            this.databaseContext = context;
        }

        @Override
        public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
            for (LinkedFile file : entry.getFiles()) {
                Optional<Path> filePath = file.findIn(databaseContext, filePreferences);
                if (filePath.isPresent()) {
                    ParserResult result = importDatabase(filePath.get());
                    if (!result.isEmpty()) {
                        return FileUtil.relativize(result.getDatabase().getEntries(), databaseContext, filePreferences);
                    }
                }
            }
            return List.of();
        }
    }
}
