package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.cleanup.RelativePathsCleanup;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Tries to import BibTeX data trying multiple [PdfImporter]s and merging the results.
/// See [PdfMergeMetadataImporter#metadataImporters] for the list of importers used.
///
/// After all importers are applied, this importer tries to fetch additional metadata for the entry using the DOI and ISBN.
public class PdfMergeMetadataImporter extends PdfImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMergeMetadataImporter.class);
    private static final Pattern FILENAME_TITLE_PATTERN = buildFilenameTitlePattern();

    private final List<PdfImporter> metadataImporters;

    private final DoiFetcher doiFetcher;
    private final ArXivFetcher arXivFetcher;
    private final IdBasedFetcher isbnFetcher;
    private final BookIsbnResolver bookIsbnResolver = new BookIsbnResolver();

    public PdfMergeMetadataImporter(ImportFormatPreferences importFormatPreferences) {
        this(importFormatPreferences, new IsbnFetcher(importFormatPreferences));
    }

    PdfMergeMetadataImporter(ImportFormatPreferences importFormatPreferences, IdBasedFetcher isbnFetcher) {
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
        doiFetcher = new DoiFetcher(importFormatPreferences);
        arXivFetcher = new ArXivFetcher(importFormatPreferences);

        this.isbnFetcher = isbnFetcher;
        // .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences))
        // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences))
    }

    private static Pattern buildFilenameTitlePattern() {
        String extensions = Arrays.stream(StandardFileType.values())
                                  .filter(type -> type != StandardFileType.ANY_FILE)
                                  .flatMap(type -> type.getExtensions().stream())
                                  .distinct()
                                  .collect(Collectors.joining("|"));
        return Pattern.compile("(?i)(.*\\.(" + extensions + ")$|microsoft (word|powerpoint|excel).*|.*\\\\.*)");
    }

    /// Makes [BibEntry] out of PDF file via merging results of several PDF analysis steps ([PdfImporter]).
    ///
    /// Algorithm:
    ///
    /// 1. Store all candidates (possible [BibEntry]ies) in a list. First elements in this list will have higher
    /// priority for merging, which means that more fields will be stored for first entries, rather than last.
    /// 2. Run [PdfImporter]s, and store extracted candidates in the list.
    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        Map<String, Optional<BibEntry>> isbnCache = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<BibEntry> extractedCandidates = extractCandidatesFromPdf(filePath, document, warnings, isbnCache);
        if (extractedCandidates.isEmpty()) {
            ParserResult result = new ParserResult();
            warnings.forEach(result::addWarning);
            return result;
        }

        List<BibEntry> fetchedCandidates = fetchIdsOfCandidates(extractedCandidates, isbnCache);

        List<BibEntry> allCandidates = new ArrayList<>(fetchedCandidates);
        allCandidates.addAll(extractedCandidates);
        BibEntry entry = mergeCandidates(allCandidates);

        // We use the absolute path here as we do not know the context where this import will be used.
        // The caller is responsible for making the path relative if necessary.
        entry.addFile(new LinkedFile("", filePath, StandardFileType.PDF.getName()));
        ParserResult result = new ParserResult(List.of(entry));
        warnings.forEach(result::addWarning);
        return result;
    }

    private List<BibEntry> extractCandidatesFromPdf(Path filePath, PDDocument document, List<String> warnings,
                                                   Map<String, Optional<BibEntry>> isbnCache) {
        List<BibEntry> candidates = new ArrayList<>();

        for (PdfImporter metadataImporter : metadataImporters) {
            try {
                ParserResult importerResult;
                if (metadataImporter instanceof PdfContentImporter contentImporter) {
                    importerResult = importContent(contentImporter, document, isbnCache);
                } else {
                    importerResult = metadataImporter.importDatabase(filePath, document);
                }
                warnings.addAll(importerResult.warnings());
                List<BibEntry> extractedEntries = importerResult.getDatabase().getEntries();
                LOGGER.debug("Importer {} extracted {}", metadataImporter.getName(), extractedEntries);
                candidates.addAll(extractedEntries);
            } catch (ParseException | IOException e) {
                LOGGER.error("Got an exception while importing PDF file", e);
            }
        }

        return candidates;
    }

    private ParserResult importContent(PdfContentImporter contentImporter, PDDocument document,
                                       Map<String, Optional<BibEntry>> isbnCache) throws IOException {
        Optional<BookExtraction> extractedBook = contentImporter.extractBook(document);
        if (extractedBook.isEmpty()) {
            return contentImporter.importPaperContent(document);
        }
        BookExtraction book = resolveBookIsbn(extractedBook.orElseThrow(), isbnCache);
        return contentImporter.importBook(book);
    }

    // [impl->req~import.pdf.book-isbn-resolution~1]
    private BookExtraction resolveBookIsbn(BookExtraction book, Map<String, Optional<BibEntry>> isbnCache) {
        if (book.localBook().isEmpty() || book.isbnCandidates().size() < 2
                || book.isbnCandidates().size() > BookIsbnResolver.MAX_CANDIDATES) {
            return book;
        }
        BibEntry localBook = book.localBook().orElseThrow();
        if (!bookIsbnResolver.hasRequiredMetadata(localBook)) {
            return book;
        }

        Map<ISBN, Optional<BibEntry>> candidates = new LinkedHashMap<>();
        for (ISBN isbn : book.isbnCandidates()) {
            candidates.put(isbn, fetchIsbn(isbn.asString(), isbnCache));
        }
        Optional<ISBN> selectedIsbn = bookIsbnResolver.resolve(localBook, candidates);
        if (selectedIsbn.isEmpty()) {
            return book;
        }
        return new BookExtraction(book.localBook(), List.of(selectedIsbn.orElseThrow()));
    }

    private Optional<BibEntry> fetchIsbn(String identifier, Map<String, Optional<BibEntry>> isbnCache) {
        return isbnCache.computeIfAbsent(identifier, id -> {
            try {
                return isbnFetcher.performSearchById(id);
            } catch (FetcherException e) {
                LOGGER.debug("Fetching failed for ISBN {}", id, e);
                return Optional.empty();
            }
        });
    }

    private List<BibEntry> fetchIdsOfCandidates(List<BibEntry> candidates, Map<String, Optional<BibEntry>> isbnCache) {
        List<BibEntry> fetchedCandidates = new ArrayList<>();

        // Collects Ids already looked for - to avoid multiple calls for one id
        final Set<String> fetchedIds = new HashSet<>();

        for (BibEntry candidate : candidates) {
            fetchData(candidate, StandardField.DOI, doiFetcher, fetchedIds, fetchedCandidates);

            // This code assumes that `eprint` field refers to an arXiv preprint, which is not correct.
            // One should also check if `archivePrefix` is equal to `arXiv`, and handle other cases too.
            fetchData(candidate, StandardField.EPRINT, arXivFetcher, fetchedIds, fetchedCandidates);

            candidate.getField(StandardField.ISBN)
                     .map(identifier -> ISBN.parse(identifier)
                                            .flatMap(ISBN::asIsbn13)
                                            .map(ISBN::asString)
                                            .orElse(identifier))
                     .filter(fetchedIds::add)
                     .flatMap(identifier -> fetchIsbn(identifier, isbnCache))
                     .ifPresent(fetchedCandidates::add);

            // TODO: Handle URLs too.
            // However, it may have problems if URL refers to the same identifier in DOI, ISBN, or arXiv.
        }

        return fetchedCandidates;
    }

    /// @param candidate         The BibEntry to look for the field
    /// @param field             The field to look for
    /// @param fetcher           The fetcher to use
    /// @param fetchedIds        The already fetched ids (will be updated)
    /// @param fetchedCandidates New candidate (will be updated)
    private void fetchData(BibEntry candidate, StandardField field, IdBasedFetcher fetcher, Set<String> fetchedIds, List<BibEntry> fetchedCandidates) {
        candidate.getField(field)
                 .filter(id -> !fetchedIds.contains(id))
                 .ifPresent(id -> {
                     fetchedIds.add(id);
                     try {
                         fetcher.performSearchById(id)
                                .ifPresent(fetchedCandidates::add);
                     } catch (FetcherException e) {
                         LOGGER.error("Fetching failed for id \"{}\".", id, e);
                     }
                 });
    }

    private static boolean isTitleLikelyFilename(String title) {
        if ((title == null) || title.isBlank()) {
            return false;
        }

        return FILENAME_TITLE_PATTERN.matcher(title.trim()).matches();
    }

    private static BibEntry mergeCandidates(List<BibEntry> candidates) {
        final BibEntry entry = new BibEntry();
        candidates.forEach(entry::mergeWith);

        if (entry.getField(StandardField.TITLE)
                 .filter(PdfMergeMetadataImporter::isTitleLikelyFilename)
                 .isPresent()) {
            candidates.stream()
                      .flatMap(candidate -> candidate.getField(StandardField.TITLE).stream())
                      .filter(candidateTitle -> !isTitleLikelyFilename(candidateTitle))
                      .findFirst()
                      .ifPresent(betterTitle -> entry.setField(StandardField.TITLE, betterTitle));
        }

        // Retain online links only
        List<LinkedFile> onlineLinks = entry.getFiles().stream().filter(LinkedFile::isOnlineLink).toList();
        entry.clearField(StandardField.FILE);
        entry.addFiles(onlineLinks);

        return entry;
    }

    /// Imports the BibTeX data from the given PDF file and relativized the paths of each linked file based on the context and the file preferences.
    public ParserResult importDatabase(Path filePath,
                                       @NonNull BibDatabaseContext context,
                                       @NonNull FilePreferences filePreferences) throws IOException {
        ParserResult parserResult = importDatabase(filePath);

        RelativePathsCleanup relativePathsCleanup = new RelativePathsCleanup(context, filePreferences);
        parserResult.getDatabase().getEntries().forEach(relativePathsCleanup::cleanup);

        return parserResult;
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
        public List<BibEntry> performSearch(@NonNull BibEntry entry) throws FetcherException {
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
