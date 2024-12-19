package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
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
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.importer.fileformat.pdf.PdfBibExtractor;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibExtractor;
import org.jabref.logic.importer.fileformat.pdf.PdfFirstPageBibExtractor;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidBibExtractor;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibExtractor;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpBibExtractor;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tries to import BibTeX data trying multiple {@link PdfBibExtractor}s and merging the results.
 * See {@link org.jabref.logic.importer.fileformat.PdfImporter#metadataImporters} for the list of importers used.
 * <p>
 * After all importers are applied, this importer tries to fetch additional metadata for the entry using the DOI and ISBN.
 */
public class PdfImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfImporter.class);

    private final ImportFormatPreferences importFormatPreferences;
    private final List<PdfBibExtractor> metadataImporters;

    public PdfImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;

        // TODO: Evaluate priorities of these {@link PdfBibExtractor}s.
        this.metadataImporters = new ArrayList<>(List.of(
                new PdfVerbatimBibExtractor(importFormatPreferences),
                new PdfEmbeddedBibExtractor(importFormatPreferences),
                new PdfXmpBibExtractor(importFormatPreferences.xmpPreferences()),
                new PdfFirstPageBibExtractor()
        ));

        if (importFormatPreferences.grobidPreferences().isGrobidEnabled()) {
            this.metadataImporters.add(2, new PdfGrobidBibExtractor(importFormatPreferences));
        }
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.readLine().startsWith("%PDF");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("PdfMergeMetadataImporter does not support importDatabase(BufferedReader reader). "
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(String data) throws IOException {
        Objects.requireNonNull(data);
        throw new UnsupportedOperationException("PdfMergeMetadataImporter does not support importDatabase(String data). "
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    /**
     * Makes {@link BibEntry} out of PDF file via merging results of several PDF analysis steps ({@link PdfBibExtractor}).
     * <p>
     * Algorithm:
     * 1. Store all candidates (possible {@link BibEntry}ies) in a list. First elements in this list will have higher
     * priority for merging, which means that more fields will be stored for first entries, rather than last.
     * 2. Run {@link PdfBibExtractor}s, and store extracted candidates in the list.
     */
    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(filePath)) {
            List<BibEntry> extractedCandidates = extractCandidatesFromPdf(filePath, document);
            if (extractedCandidates.isEmpty()) {
                return new ParserResult();
            }

            List<BibEntry> fetchedCandidates = fetchIdsOfCandidates(extractedCandidates);

            Stream<BibEntry> allCandidates = Stream.concat(fetchedCandidates.stream(), extractedCandidates.stream());
            BibEntry entry = mergeCandidates(allCandidates);

            // We use the absolute path here as we do not know the context where this import will be used.
            // The caller is responsible for making the path relative if necessary.
            entry.addFile(new LinkedFile("", filePath, StandardFileType.PDF.getName()));
            return new ParserResult(List.of(entry));
        } catch (EncryptedPdfsNotSupportedException e) {
            return ParserResult.fromErrorMessage(Localization.lang("Decryption not supported."));
        }
    }

    private List<BibEntry> extractCandidatesFromPdf(Path filePath, PDDocument document) {
        List<BibEntry> candidates = new ArrayList<>();

        for (PdfBibExtractor metadataImporter : metadataImporters) {
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

    /**
     * A modified version of {@link PdfImporter#importDatabase(Path)}, but it
     * relativizes the {@code filePath} if there are working directories before parsing it
     * into {@link PdfImporter#importDatabase(Path)}
     * (Otherwise no path modification happens).
     *
     * @param filePath    The unrelativized {@code filePath}.
     */
    public ParserResult importDatabase(Path filePath, BibDatabaseContext context, FilePreferences filePreferences) throws IOException {
        Objects.requireNonNull(context);
        Objects.requireNonNull(filePreferences);

        List<Path> directories = context.getFileDirectories(filePreferences);

        filePath = FileUtil.relativize(filePath, directories);

        return importDatabase(filePath);
    }

    @Override
    public String getName() {
        return "PDF meta data merger";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.PDF;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Imports BibTeX data from a PDF using multiple strategies (e.g., XMP, embedded BibTeX, text parsing, Grobid, and DOI lookup) and merges the result.");
    }

    public static class EntryBasedFetcherWrapper extends PdfImporter implements EntryBasedFetcher {

        private static final Logger LOGGER = LoggerFactory.getLogger(EntryBasedFetcherWrapper.class);
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
                    try {
                        ParserResult result = importDatabase(filePath.get());
                        if (!result.isEmpty()) {
                            return FileUtil.relativize(result.getDatabase().getEntries(), databaseContext, filePreferences);
                        }
                    } catch (IOException e) {
                        LOGGER.error("Cannot read {}", filePath.get(), e);
                    }
                }
            }
            return List.of();
        }
    }
}
