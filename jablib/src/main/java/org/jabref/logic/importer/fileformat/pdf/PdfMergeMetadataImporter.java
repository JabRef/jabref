package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import org.jabref.logic.util.PdfUtils;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import com.google.common.annotations.VisibleForTesting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
    private final IsbnFetcher isbnFetcher;

    public PdfMergeMetadataImporter(ImportFormatPreferences importFormatPreferences) {
        // TODO: Evaluate priorities of these {@link PdfBibExtractor}s.
        this.metadataImporters = new ArrayList<>(List.of(
                new PdfVerbatimBibtexImporter(importFormatPreferences),
                new PdfEmbeddedBibFileImporter(importFormatPreferences),
                new PdfXmpImporter(importFormatPreferences.xmpPreferences()),
                new PdfContentImporter()
        ));

        if (importFormatPreferences.grobidPreferences().isGrobidEnabled()) {
            this.metadataImporters.add(2, new PdfGrobidHeaderImporter(importFormatPreferences));
        }
        doiFetcher = new DoiFetcher(importFormatPreferences);
        arXivFetcher = new ArXivFetcher(importFormatPreferences);

        isbnFetcher = new IsbnFetcher(importFormatPreferences);
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
        List<String> warnings = new ArrayList<>();
        List<BibEntry> extractedCandidates = extractCandidatesFromPdf(filePath, document, warnings);
        if (extractedCandidates.isEmpty()) {
            ParserResult result = new ParserResult();
            warnings.forEach(result::addWarning);
            return result;
        }

        List<BibEntry> fetchedCandidates = fetchIdsOfCandidates(extractedCandidates);

        List<BibEntry> allCandidates = new ArrayList<>(fetchedCandidates);
        allCandidates.addAll(extractedCandidates);
        BibEntry entry = mergeCandidates(allCandidates, extractLeadingPagesText(document));

        // We use the absolute path here as we do not know the context where this import will be used.
        // The caller is responsible for making the path relative if necessary.
        entry.addFile(new LinkedFile("", filePath, StandardFileType.PDF.getName()));
        ParserResult result = new ParserResult(List.of(entry));
        warnings.forEach(result::addWarning);
        return result;
    }

    /// Text of the first two pages for the author cross-check; empty if the text cannot be extracted
    private static String extractLeadingPagesText(PDDocument document) {
        try {
            return PdfUtils.getLeadingPagesContents(document, 2);
        } catch (IOException e) {
            LOGGER.debug("Could not extract text for the author plausibility check", e);
            return "";
        }
    }

    private List<BibEntry> extractCandidatesFromPdf(Path filePath, PDDocument document, List<String> warnings) {
        List<BibEntry> candidates = new ArrayList<>();

        for (PdfImporter metadataImporter : metadataImporters) {
            try {
                ParserResult importerResult = metadataImporter.importDatabase(filePath, document);
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

    private List<BibEntry> fetchIdsOfCandidates(List<BibEntry> candidates) {
        List<BibEntry> fetchedCandidates = new ArrayList<>();

        // Collects Ids already looked for - to avoid multiple calls for one id
        final Set<String> fetchedIds = new HashSet<>();

        for (BibEntry candidate : candidates) {
            fetchData(candidate, StandardField.DOI, doiFetcher, fetchedIds, fetchedCandidates);

            // This code assumes that `eprint` field refers to an arXiv preprint, which is not correct.
            // One should also check if `archivePrefix` is equal to `arXiv`, and handle other cases too.
            fetchData(candidate, StandardField.EPRINT, arXivFetcher, fetchedIds, fetchedCandidates);

            fetchData(candidate, StandardField.ISBN, isbnFetcher, fetchedIds, fetchedCandidates);

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

    /// Merges PDF metadata candidates in priority order, replaces filename-like titles, checks the author
    /// against the PDF text, and retains only online file links.
    ///
    /// @param candidates       candidate entries ordered by descending priority
    /// @param leadingPagesText text of the PDF's leading pages; `null` or empty leaves the author unchanged
    @VisibleForTesting
    static BibEntry mergeCandidates(List<BibEntry> candidates, @Nullable String leadingPagesText) {
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

        PdfAuthorCrossCheck.crossCheckAuthor(entry, candidates, leadingPagesText);

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
