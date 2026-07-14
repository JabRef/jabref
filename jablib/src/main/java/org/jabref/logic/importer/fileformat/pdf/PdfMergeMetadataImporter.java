package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import com.google.common.annotations.VisibleForTesting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Tries to import BibTeX data trying multiple {@link PdfImporter}s and merging the results.
/// See {@link PdfMergeMetadataImporter#metadataImporters} for the list of importers used.
///
/// After all importers are applied, this importer tries to fetch additional metadata for the entry using the DOI and ISBN.
public class PdfMergeMetadataImporter extends PdfImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMergeMetadataImporter.class);
    private static final Pattern FILENAME_TITLE_PATTERN = buildFilenameTitlePattern();
    private static final Pattern LEADING_AND_TRAILING_NON_LETTERS = Pattern.compile("^\\P{L}+|\\P{L}+$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_LETTERS = Pattern.compile("\\P{L}+");
    private static final Pattern SOFT_LINE_BREAK_HYPHEN = Pattern.compile("-\\r?\\n\\s*");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Set<String> NAME_LIST_LOWERCASE_WORDS = Set.of(
            "and", "van", "von", "der", "den", "de", "del", "dos", "da", "di", "la", "le", "ten", "ter", "y", "e");

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

    private static Pattern buildFilenameTitlePattern() {
        String extensions = Arrays.stream(StandardFileType.values())
                                  .filter(type -> type != StandardFileType.ANY_FILE)
                                  .flatMap(type -> type.getExtensions().stream())
                                  .distinct()
                                  .collect(Collectors.joining("|"));
        return Pattern.compile("(?i)(.*\\.(" + extensions + ")$|microsoft (word|powerpoint|excel).*|.*\\\\.*)");
    }

    /// Makes {@link BibEntry} out of PDF file via merging results of several PDF analysis steps ({@link PdfImporter}).
    ///
    /// Algorithm:
    /// 1. Store all candidates (possible {@link BibEntry}ies) in a list. First elements in this list will have higher
    /// priority for merging, which means that more fields will be stored for first entries, rather than last.
    /// 2. Run {@link PdfImporter}s, and store extracted candidates in the list.
    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        List<BibEntry> extractedCandidates = extractCandidatesFromPdf(filePath, document);
        if (extractedCandidates.isEmpty()) {
            return new ParserResult();
        }

        List<BibEntry> fetchedCandidates = fetchIdsOfCandidates(extractedCandidates);

        List<BibEntry> allCandidates = new ArrayList<>(fetchedCandidates);
        allCandidates.addAll(extractedCandidates);
        BibEntry entry = mergeCandidates(allCandidates, extractLeadingPagesText(document));

        // We use the absolute path here as we do not know the context where this import will be used.
        // The caller is responsible for making the path relative if necessary.
        entry.addFile(new LinkedFile("", filePath, StandardFileType.PDF.getName()));
        return new ParserResult(List.of(entry));
    }

    private List<BibEntry> extractCandidatesFromPdf(Path filePath, PDDocument document) {
        List<BibEntry> candidates = new ArrayList<>();

        for (PdfImporter metadataImporter : metadataImporters) {
            try {
                List<BibEntry> extractedEntries = metadataImporter.importDatabase(filePath, document).getDatabase().getEntries();
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

    @VisibleForTesting
    static BibEntry mergeCandidates(List<BibEntry> candidates, String documentText) {
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

        crossCheckAuthor(entry, candidates, documentText);

        // Retain online links only
        List<LinkedFile> onlineLinks = entry.getFiles().stream().filter(LinkedFile::isOnlineLink).toList();
        entry.clearField(StandardField.FILE);
        entry.addFiles(onlineLinks);

        return entry;
    }

    /// Office suites store the account name of whoever produced the file as "Author" in the document
    /// information dictionary. Via the XMP/docinfo candidate, this person — usually not an author of the
    /// work at all — would win the merge against author lists extracted from the document itself.
    ///
    /// Since scholarly works print their authors on the leading pages, an author list is trusted only if at
    /// least one of its family names occurs in the text of those pages. An unconfirmed merged value is
    /// replaced by the best-confirmed candidate value. If no candidate is confirmed, a single person coming
    /// from a non-bibliographic candidate (no citation key, no explicit entry type) is dropped entirely: a
    /// wrong author is worse than none. Entries with a citation key or explicit type are left untouched so
    /// that metadata previously written by JabRef survives re-import even when the PDF text does not
    /// contain the author (e.g. slides or reports).
    private static void crossCheckAuthor(BibEntry entry, List<BibEntry> candidates, String documentText) {
        if (documentText.isBlank()) {
            return;
        }
        entry.getField(StandardField.AUTHOR).ifPresent(mergedAuthor -> {
            String normalizedText = normalizeForComparison(documentText);
            if (isAuthorConfirmedByText(mergedAuthor, normalizedText)) {
                return;
            }

            candidates.stream()
                      .flatMap(candidate -> candidate.getField(StandardField.AUTHOR).stream())
                      .filter(PdfMergeMetadataImporter::looksLikeAuthorList)
                      .map(author -> new ScoredAuthor(author, countFamilyNamesInText(author, normalizedText)))
                      .filter(scored -> scored.confirmedNames() > 0)
                      // keeps the earlier (= higher-priority) candidate unless a later one is strictly better
                      .reduce((first, second) -> second.confirmedNames() > first.confirmedNames() ? second : first)
                      .ifPresentOrElse(
                              scored -> entry.setField(StandardField.AUTHOR, scored.value()),
                              () -> dropCreatorOnlyAuthor(entry, candidates, mergedAuthor));
        });
    }

    private static void dropCreatorOnlyAuthor(BibEntry entry, List<BibEntry> candidates, String mergedAuthor) {
        // The merge is first-wins, thus the merged value stems from the first candidate carrying an author
        boolean sourceLooksBibliographic = candidates.stream()
                                                     .filter(candidate -> candidate.hasField(StandardField.AUTHOR))
                                                     .findFirst()
                                                     .map(source -> source.getCitationKey().isPresent()
                                                             || !BibEntry.DEFAULT_TYPE.equals(source.getType()))
                                                     .orElse(true);
        if (!sourceLooksBibliographic && (AuthorList.parse(mergedAuthor).getNumberOfAuthors() == 1)) {
            entry.clearField(StandardField.AUTHOR);
        }
    }

    private record ScoredAuthor(String value, int confirmedNames) {
    }

    /// Keeps sentence fragments mis-parsed as author lists (e.g. by [PdfContentImporter]) from being
    /// promoted by the cross-check: their words trivially occur in the document text. Real name lists
    /// consist of capitalized words plus name particles and the "and" separator, while prose contains
    /// other lowercase words and non-letter tokens such as URLs.
    private static boolean looksLikeAuthorList(String authorField) {
        for (String word : WHITESPACE.split(authorField)) {
            String stripped = LEADING_AND_TRAILING_NON_LETTERS.matcher(word).replaceAll("");
            if (stripped.isEmpty()
                    || (!Character.isUpperCase(stripped.codePointAt(0)) && !NAME_LIST_LOWERCASE_WORDS.contains(stripped))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAuthorConfirmedByText(String authorField, String normalizedText) {
        if (countFamilyNamesInText(authorField, normalizedText) > 0) {
            return true;
        }
        // Fallback for author values AuthorList cannot split into proper persons (e.g. exotic separator
        // characters from broken XMP decoding): any word of the raw value found in the text confirms it.
        return Arrays.stream(NON_LETTERS.split(authorField))
                     .filter(word -> word.length() >= 2)
                     .map(PdfMergeMetadataImporter::normalizeForComparison)
                     .anyMatch(word -> !word.isBlank() && containsWord(normalizedText, word));
    }

    private static int countFamilyNamesInText(String authorField, String normalizedText) {
        return (int) AuthorList.parse(authorField).getAuthors().stream()
                               .map(Author::getFamilyName)
                               .flatMap(Optional::stream)
                               .map(PdfMergeMetadataImporter::normalizeForComparison)
                               .filter(familyName -> !familyName.isBlank() && containsWord(normalizedText, familyName))
                               .count();
    }

    /// Whole-word check: the occurrence must not be preceded or followed by another letter
    private static boolean containsWord(String normalizedText, String word) {
        int index = normalizedText.indexOf(word);
        while (index >= 0) {
            int end = index + word.length();
            if ((index == 0 || !Character.isLetter(normalizedText.codePointBefore(index)))
                    && (end >= normalizedText.length() || !Character.isLetter(normalizedText.codePointAt(end)))) {
                return true;
            }
            index = normalizedText.indexOf(word, index + 1);
        }
        return false;
    }

    /// Case-, diacritic- and hyphen-insensitive comparison form. Hyphens are removed on both sides because
    /// text extraction may break a name at the end of a justified line ("Breitenbü-\ncher"), where the
    /// hyphen is a soft line-break hyphen for one name but a genuine part of another (e.g. "Kylo-Ren").
    private static String normalizeForComparison(String text) {
        String dehyphenated = SOFT_LINE_BREAK_HYPHEN.matcher(text).replaceAll("").replace("-", "");
        return COMBINING_MARKS.matcher(Normalizer.normalize(dehyphenated, Normalizer.Form.NFKD))
                              .replaceAll("")
                              .toLowerCase(Locale.ROOT);
    }

    private static String extractLeadingPagesText(PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(2, document.getNumberOfPages()));
            return stripper.getText(document);
        } catch (IOException e) {
            LOGGER.debug("Could not extract text for the author plausibility check", e);
            return "";
        }
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
