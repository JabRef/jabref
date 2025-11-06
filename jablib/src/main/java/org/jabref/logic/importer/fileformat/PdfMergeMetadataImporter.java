package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibtexImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NonNull;
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
            this.metadataImporters.add(2, new PdfGrobidImporter(importFormatPreferences));
        }
        doiFetcher = new DoiFetcher(importFormatPreferences);
        arXivFetcher = new ArXivFetcher(importFormatPreferences);

        isbnFetcher = new IsbnFetcher(importFormatPreferences);
        // .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences))
        // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences))
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

    /**
     * @param candidate         The BibEntry to look for the field
     * @param field             The field to look for
     * @param fetcher           The fetcher to use
     * @param fetchedIds        The already fetched ids (will be updated)
     * @param fetchedCandidates New candidate (will be updated)
     */
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

    private static BibEntry mergeCandidates(Stream<BibEntry> candidates) {
        // Collect all candidate entries
        List<BibEntry> allCandidates = candidates.toList();

        // Merge all fields from candidates into one entry
        final BibEntry entry = new BibEntry();
        allCandidates.forEach(entry::mergeWith);

        // Retain only online links
        List<LinkedFile> onlineLinks = entry.getFiles().stream().filter(LinkedFile::isOnlineLink).toList();
        entry.clearField(StandardField.FILE);
        entry.addFiles(onlineLinks);

        // === NEW PART ===
        // Step 1: get the current merged title
        Optional<String> currentTitle = entry.getField(StandardField.TITLE);

        // Step 2: find any other "better" title from the candidates
        Optional<String> betterTitle = allCandidates.stream()
                                                    .map(e -> e.getField(StandardField.TITLE))
                                                    .flatMap(Optional::stream)
                                                    .filter(t -> isBetterTitle(t, currentTitle.orElse("")))
                                                    .findFirst();

        // Step 3: replace the title if a better one was found
        betterTitle.ifPresent(title -> {
            entry.setField(StandardField.TITLE, title);
            LOGGER.debug("Replaced title with better one: {}", title);
        });

        return entry;
    }

    /**
     * Decide if a new title looks "better" than the old one.
     * Very basic heuristic: longer, contains spaces, and not a generic filename.
     */
    public static boolean isBetterTitle(String newTitle, String oldTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            return false;
        }

        newTitle = newTitle.trim();
        oldTitle = oldTitle == null ? "" : oldTitle.trim();

        String lower = newTitle.toLowerCase();

        // 1. Exclude parasites titles
        if (lower.matches(".*(microsoft word|adobe acrobat|document|untitled|journal template).*")) {
            return false;
        }
        if (lower.matches(".*\\.(pdf|docx?|tex|rtf|zip|txt)$")) {
            return false;
        }

        // 2. Exclude titles too short or with blank
        if (newTitle.split("\\s+").length < 3) {
            return false;
        }

        // 3. Exclude titles that could be path of the file
        if (newTitle.contains(":\\") || newTitle.contains("/") || newTitle.contains("\\")) {
            return false;
        }

        // 4. Check if there is a title style
        boolean hasCapitalizedWords = Pattern.compile("\\b[A-Z][a-z]+").matcher(newTitle).find();
        boolean allUppercase = newTitle.equals(newTitle.toUpperCase());
        boolean allLowercase = newTitle.equals(newTitle.toLowerCase());

        if (!hasCapitalizedWords || allUppercase || allLowercase) {
            // Exclude if everything is upper case or lower case
            return false;
        }

        // 5. Better grade if it uses punctuation ("-", ":", ",")
        int punctuationScore = 0;
        for (char c : newTitle.toCharArray()) {
            if (c == ':' || c == '-' || c == ',') {
                punctuationScore++;
            }
        }

        // 6. Evaluate with a longer size (with a certain limit)
        boolean longer = newTitle.length() > oldTitle.length() + 5 && newTitle.length() < 300;

        // 7. Choose the better one
        int score = 0;
        if (longer) {
            score++;
        }
        if (punctuationScore > 0) {
            score++;
        }
        if (Character.isUpperCase(newTitle.charAt(0))) {
            score++;
        }

        return score >= 2;
    }

    /**
     * Imports the BibTeX data from the given PDF file and relativized the paths of each linked file based on the context and the file preferences.
     */
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
