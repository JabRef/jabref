package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileNameUniqueness;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Splits a PDF into one file and one entry per paper, using the PDF outline.
///
/// The papers are the first outline level with more than one bookmark; bookmarks nested inside a paper are its sections.
/// When some papers have sections, the bookmarks on that level without sections are front or back matter and are not split out.
/// A PDF whose outline does not mark at least two papers is left unchanged:
/// the outline is the only place the file itself says where each paper starts.
///
/// A single paper's outline can look like a volume's, with its own sections in place of papers.
/// So the outline is only trusted when most papers start on a title page and, if they have sections, share a section name.
@NullMarked
public final class PdfOutlineSplitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfOutlineSplitter.class);

    private static final Pattern SECTION_NUMBER = Pattern.compile("^[0-9]+(\\.[0-9]+)*\\.?\\s+");

    private static final Set<EntryType> PROCEEDINGS_TYPES = Set.of(StandardEntryType.Proceedings, StandardEntryType.MvProceedings);

    private PdfOutlineSplitter() {
    }

    public static boolean isProceedings(EntryType type) {
        return PROCEEDINGS_TYPES.contains(type);
    }

    /// Writes one PDF per paper into `targetDirectory` and returns an entry for each.
    /// The file at `source` is not modified.
    ///
    /// @return empty when the outline does not mark at least two papers, or the PDF holds a single paper
    public static List<BibEntry> split(Path source, BibEntry parent, Path targetDirectory) throws IOException {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            List<Bookmark> bookmarks = new ArrayList<>(paperBookmarks(document));
            bookmarks.sort(Comparator.comparingInt(Bookmark::page));
            List<Bookmark> bookmarksInOrder = distinctPages(bookmarks);
            // Where papers list their sections, a bookmark without any is front or back matter, such as the preface
            boolean papersHaveSections = bookmarksInOrder.stream().anyMatch(Bookmark::hasSections);
            List<Bookmark> paperStarts = bookmarksInOrder.stream().filter(bookmark -> !papersHaveSections || bookmark.hasSections()).toList();
            if ((paperStarts.size() < 2) || !isVolumeOfPapers(paperStarts, PaperTitlePages.of(document))) {
                return List.of();
            }

            int pageCount = document.getNumberOfPages();
            int offset = PdfLocationResolver.pageOffset(parent);
            List<BibEntry> papers = new ArrayList<>();
            for (int index = 0; index < bookmarksInOrder.size(); index++) {
                Bookmark bookmark = bookmarksInOrder.get(index);
                int lastPdfPage = index + 1 < bookmarksInOrder.size() ? bookmarksInOrder.get(index + 1).page() - 1 : pageCount;
                int printedFirst = bookmark.page() - offset;
                int printedLast = lastPdfPage - offset;
                if ((papersHaveSections && !bookmark.hasSections()) || (printedFirst < 1)) {
                    continue;
                }

                String fileName = FileUtil.getValidFileName(bookmark.title() + ".pdf");
                Path target = targetDirectory.resolve(FileNameUniqueness.generateUniqueFileName(targetDirectory, fileName));
                PdfPageCopier.copy(document, bookmark.page(), lastPdfPage, target);
                papers.add(paperEntry(parent, bookmark.title(), printedFirst, printedLast, target));
            }
            return papers;
        }
    }

    private static boolean isVolumeOfPapers(List<Bookmark> paperStarts, PaperTitlePages titlePages) throws IOException {
        int onTitlePage = 0;
        for (Bookmark paper : paperStarts) {
            if (titlePages.startsPaper(paper.page())) {
                onTitlePage++;
            }
        }
        if (!isMostOf(onTitlePage, paperStarts.size())) {
            return false;
        }
        return !paperStarts.getFirst().hasSections() || shareASection(paperStarts);
    }

    /// Papers in a volume repeat section names such as "Introduction" or "References"; the sections of one paper do not
    private static boolean shareASection(List<Bookmark> paperStarts) {
        Map<String, Long> papersPerSection = paperStarts.stream()
                                                        .flatMap(paper -> paper.sections().stream().map(PdfOutlineSplitter::sectionName).distinct())
                                                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return papersPerSection.values().stream().anyMatch(papers -> isMostOf(papers, paperStarts.size()));
    }

    private static String sectionName(String sectionTitle) {
        return SECTION_NUMBER.matcher(sectionTitle).replaceFirst("").toLowerCase(Locale.ROOT);
    }

    /// At least two, and at least half of all
    private static boolean isMostOf(long count, int total) {
        return (count >= 2) && (count * 2 >= total);
    }

    private static BibEntry paperEntry(BibEntry parent, String title, int printedFirst, int printedLast, Path file) {
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.TITLE, title)
                .withField(StandardField.PAGES, pagesValue(printedFirst, printedLast))
                .withFiles(List.of(new LinkedFile(title, file.toAbsolutePath(), StandardFileType.PDF.getName())));
        parent.getCitationKey().ifPresent(key -> paper.withField(StandardField.CROSSREF, key));
        if (printedFirst != 1) {
            paper.withField(PdfLocationResolver.PAGE_OFFSET, Integer.toString(1 - printedFirst));
        }
        return paper;
    }

    private static String pagesValue(int first, int last) {
        if (first == last) {
            return Integer.toString(first);
        }
        return first + "--" + last;
    }

    private static List<Bookmark> paperBookmarks(PDDocument document) {
        return Optional.ofNullable(document.getDocumentCatalog().getDocumentOutline())
                       .map(outline -> paperLevel(outline, document))
                       .orElse(List.of());
    }

    private static List<Bookmark> paperLevel(PDDocumentOutline outline, PDDocument document) {
        Set<PDOutlineItem> seen = new HashSet<>();
        List<PDOutlineItem> level = children(outline, seen);
        // A lone bookmark around the whole outline is the volume or a part, not a paper
        while ((level.size() == 1) && level.getFirst().hasChildren()) {
            level = children(level.getFirst(), seen);
        }
        List<Bookmark> bookmarks = new ArrayList<>();
        for (PDOutlineItem item : level) {
            List<String> sections = children(item, seen).stream().map(PdfOutlineSplitter::title).toList();
            pageNumber(item, document).ifPresent(page -> bookmarks.add(new Bookmark(title(item), page, sections)));
        }
        return bookmarks;
    }

    /// Stops at a bookmark that was already visited, as a damaged outline can link back to itself
    private static List<PDOutlineItem> children(PDOutlineNode node, Set<PDOutlineItem> seen) {
        List<PDOutlineItem> children = new ArrayList<>();
        for (PDOutlineItem child : node.children()) {
            if (!seen.add(child)) {
                break;
            }
            children.add(child);
        }
        return children;
    }

    private static List<Bookmark> distinctPages(List<Bookmark> bookmarks) {
        List<Bookmark> distinct = new ArrayList<>();
        for (Bookmark bookmark : bookmarks) {
            if (distinct.isEmpty() || (distinct.getLast().page() != bookmark.page())) {
                distinct.add(bookmark);
            }
        }
        return distinct;
    }

    private static Optional<Integer> pageNumber(PDOutlineItem item, PDDocument document) {
        try {
            return Optional.ofNullable(item.findDestinationPage(document))
                           .map(page -> document.getPages().indexOf(page) + 1)
                           .filter(page -> page > 0);
        } catch (IOException e) {
            LOGGER.debug("Skipping an outline item that has no page", e);
            return Optional.empty();
        }
    }

    private static String title(PDOutlineItem item) {
        return Optional.ofNullable(item.getTitle())
                       .filter(StringUtil::isNotBlank)
                       .map(String::trim)
                       .orElse("Untitled");
    }

    private record Bookmark(String title, int page, List<String> sections) {
        boolean hasSections() {
            return !sections.isEmpty();
        }
    }
}
