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
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDPageLabels;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->req~logic.pdf.split-proceedings-outline~1]
// [impl->adr~split-multipaper-pdfs-by-outline~1]
@NullMarked
public final class PdfOutlineSplitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfOutlineSplitter.class);

    private static final Pattern SECTION_NUMBER = Pattern.compile("^[0-9]+(\\.[0-9]+)*\\.?\\s+");

    private static final Pattern PRINTED_PAGE = Pattern.compile("[0-9]{1,9}");

    private static final Map<EntryType, EntryType> PAPER_TYPES = Map.of(
            StandardEntryType.Proceedings, StandardEntryType.InProceedings,
            StandardEntryType.MvProceedings, StandardEntryType.InProceedings,
            StandardEntryType.Collection, StandardEntryType.InCollection,
            StandardEntryType.MvCollection, StandardEntryType.InCollection,
            StandardEntryType.Book, StandardEntryType.InBook,
            StandardEntryType.MvBook, StandardEntryType.InBook);

    private static final Pattern PROCEEDINGS_TITLE = Pattern.compile("proceedings|conference|workshop|symposium", Pattern.CASE_INSENSITIVE);

    private static final Pattern PROCEEDINGS_LINE = Pattern.compile("^\\s*Proceedings\\b", Pattern.MULTILINE);

    private static final Pattern ORGANIZATION = Pattern.compile("organi[sz]ation|.*committees?", Pattern.CASE_INSENSITIVE);

    private static final Pattern EDITORS = Pattern.compile("\\(eds?\\.\\)|edited\\s*by|^\\s*(volume\\s+)?editors?\\s*$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static final Pattern CONTRIBUTORS = Pattern.compile("author\\s+index|(list\\s+of\\s+)?contributors", Pattern.CASE_INSENSITIVE);

    private PdfOutlineSplitter() {
    }

    public static boolean holdsPapers(EntryType type) {
        return PAPER_TYPES.containsKey(type);
    }

    public static EntryType paperType(EntryType volumeType) {
        return PAPER_TYPES.getOrDefault(volumeType, StandardEntryType.InBook);
    }

    /// @return no papers when the outline does not mark at least two papers, or the PDF holds a single paper
    public static SplitPapers split(Path source, BibEntry parent, Path targetDirectory) throws IOException {
        Optional<Volume> volume = readVolume(source);
        if (volume.isEmpty()) {
            return new SplitPapers(List.of(), 0);
        }
        return split(source, parent, volume.orElseThrow(), targetDirectory);
    }

    /// @param volume the papers that [#readVolume(Path)] found in `source`
    public static SplitPapers split(Path source, BibEntry parent, Volume volume, Path targetDirectory) throws IOException {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            int offset = parent.hasField(PdfLocationResolver.PAGE_OFFSET) ? PdfLocationResolver.pageOffset(parent) : volume.printedPageOffset();
            List<BibEntry> papers = new ArrayList<>();
            for (PaperPages paper : volume.papers()) {
                int printedFirst = paper.firstPage() - offset;
                int printedLast = paper.lastPage() - offset;
                if (printedFirst < 1) {
                    continue;
                }

                Path target = PdfPageCopier.createUniqueFile(targetDirectory, FileUtil.getValidFileName(paper.title() + ".pdf"));
                PdfPageCopier.copy(document, paper.firstPage(), paper.lastPage(), target);
                papers.add(paperEntry(parent, paper.title(), printedFirst, printedLast, target));
            }
            return new SplitPapers(papers, offset);
        }
    }

    /// @return empty when the outline does not mark at least two papers, or the PDF holds a single paper
    public static Optional<Volume> readVolume(Path source) throws IOException {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            return readVolume(document);
        }
    }

    public static Optional<Volume> readVolume(PDDocument document) throws IOException {
        List<Bookmark> bookmarks = new ArrayList<>(paperBookmarks(document));
        bookmarks.sort(Comparator.comparingInt(Bookmark::page));
        List<Bookmark> bookmarksInOrder = distinctPages(bookmarks);
        boolean papersHaveSections = bookmarksInOrder.stream().anyMatch(Bookmark::hasSections);
        List<Bookmark> paperStarts = bookmarksInOrder.stream().filter(bookmark -> !papersHaveSections || bookmark.hasSections()).toList();
        if ((paperStarts.size() < 2) || (papersHaveSections && !shareASection(paperStarts))) {
            return Optional.empty();
        }
        PaperTitlePages titlePages = PaperTitlePages.of(document, paperStarts.stream().map(Bookmark::page).toList());
        if (!startOnTitlePages(paperStarts, titlePages)) {
            return Optional.empty();
        }

        int pageCount = document.getNumberOfPages();
        List<PaperPages> papers = new ArrayList<>();
        for (int index = 0; index < bookmarksInOrder.size(); index++) {
            Bookmark bookmark = bookmarksInOrder.get(index);
            if (papersHaveSections && !bookmark.hasSections()) {
                continue;
            }
            int lastPage = index + 1 < bookmarksInOrder.size() ? bookmarksInOrder.get(index + 1).page() - 1 : pageCount;
            papers.add(new PaperPages(bookmark.title(), bookmark.page(), lastPage));
        }
        return Optional.of(new Volume(volumeType(document, bookmarksInOrder, paperStarts, titlePages), papers, printedPageOffset(document, papers)));
    }

    private static int printedPageOffset(PDDocument document, List<PaperPages> papers) throws IOException {
        String[] labels = Optional.ofNullable(document.getDocumentCatalog().getPageLabels())
                                  .map(PDPageLabels::getLabelsByPageIndices)
                                  .orElse(new String[0]);
        Set<Optional<Integer>> offsets = papers.stream()
                                               .map(paper -> printedPage(labels, paper.firstPage()).map(printed -> paper.firstPage() - printed))
                                               .collect(Collectors.toSet());
        return offsets.size() == 1 ? offsets.iterator().next().orElse(0) : 0;
    }

    private static Optional<Integer> printedPage(String[] labels, int pdfPage) {
        if (pdfPage > labels.length) {
            return Optional.empty();
        }
        return Optional.ofNullable(labels[pdfPage - 1])
                       .filter(label -> PRINTED_PAGE.matcher(label).matches())
                       .map(Integer::parseInt);
    }

    private static EntryType volumeType(PDDocument document, List<Bookmark> bookmarks, List<Bookmark> paperStarts, PaperTitlePages titlePages) throws IOException {
        int firstPaperPage = paperStarts.getFirst().page();
        StringBuilder frontMatterTitles = new StringBuilder();
        for (int page = 1; page < firstPaperPage; page++) {
            frontMatterTitles.append(titlePages.title(page));
        }
        String titlePagesText = titlePagesText(document, bookmarks, firstPaperPage);
        List<String> matterBookmarks = bookmarks.stream()
                                                .filter(bookmark -> !paperStarts.contains(bookmark))
                                                .map(Bookmark::title)
                                                .toList();
        if (PROCEEDINGS_TITLE.matcher(frontMatterTitles).find()
                || PROCEEDINGS_LINE.matcher(titlePagesText).find()
                || matterBookmarks.stream().anyMatch(title -> ORGANIZATION.matcher(title).matches())) {
            return StandardEntryType.Proceedings;
        }
        long papersWithAuthorAddresses = paperStarts.stream().filter(paper -> titlePages.givesAuthorAddresses(paper.page())).count();
        if (EDITORS.matcher(frontMatterTitles).find()
                || EDITORS.matcher(titlePagesText).find()
                || matterBookmarks.stream().anyMatch(title -> CONTRIBUTORS.matcher(title).matches())
                || isMostOf(papersWithAuthorAddresses, paperStarts.size())) {
            return StandardEntryType.Collection;
        }
        return StandardEntryType.Book;
    }

    private static String titlePagesText(PDDocument document, List<Bookmark> bookmarks, int firstPaperPage) throws IOException {
        int firstBookmarkAfterCover = bookmarks.stream().mapToInt(Bookmark::page).filter(page -> page > 1).findFirst().orElse(firstPaperPage);
        int lastTitlePage = Math.min(firstBookmarkAfterCover, firstPaperPage) - 1;
        if (lastTitlePage < 1) {
            return "";
        }
        PDFTextStripper stripper = new InterruptiblePDFTextStripper();
        stripper.setStartPage(1);
        stripper.setEndPage(lastTitlePage);
        return stripper.getText(document);
    }

    private static boolean startOnTitlePages(List<Bookmark> paperStarts, PaperTitlePages titlePages) throws IOException {
        int onTitlePage = 0;
        for (Bookmark paper : paperStarts) {
            if (titlePages.startsPaper(paper.page())) {
                onTitlePage++;
            }
        }
        return isMostOf(onTitlePage, paperStarts.size());
    }

    private static boolean shareASection(List<Bookmark> paperStarts) {
        Map<String, Long> papersPerSection = paperStarts.stream()
                                                        .flatMap(paper -> paper.sections().stream().map(PdfOutlineSplitter::sectionName).distinct())
                                                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return papersPerSection.values().stream().anyMatch(papers -> isMostOf(papers, paperStarts.size()));
    }

    private static String sectionName(String sectionTitle) {
        return SECTION_NUMBER.matcher(sectionTitle).replaceFirst("").toLowerCase(Locale.ROOT);
    }

    private static boolean isMostOf(long count, int total) {
        return (count >= 2) && (count * 2 >= total);
    }

    private static BibEntry paperEntry(BibEntry parent, String title, int printedFirst, int printedLast, Path file) {
        BibEntry paper = new BibEntry(paperType(parent.getType()))
                .withField(StandardField.TITLE, title)
                .withField(StandardField.PAGES, pagesValue(printedFirst, printedLast))
                .withFiles(List.of(new LinkedFile(title, file.toAbsolutePath(), StandardFileType.PDF.getName())));
        // [impl->req~logic.pdf.paper-entry-crossref~1]
        parent.getCitationKey().ifPresent(key -> paper.withField(StandardField.CROSSREF, key));
        return paper.withField(PdfLocationResolver.PAGE_OFFSET, Integer.toString(1 - printedFirst));
    }

    public static String pagesValue(int first, int last) {
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
            } else if (!distinct.getLast().hasSections() && bookmark.hasSections()) {
                distinct.set(distinct.size() - 1, bookmark);
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

    public record PaperPages(String title, int firstPage, int lastPage) {
    }

    /// @param type              proceedings, a collection, or a book
    /// @param printedPageOffset the PDF page minus the printed page number, zero when the PDF does not label its pages
    public record Volume(EntryType type, List<PaperPages> papers, int printedPageOffset) {
    }

    /// @param pageOffset the PDF page minus the printed page number that the papers' pages were read with
    public record SplitPapers(List<BibEntry> papers, int pageOffset) {
    }

    private record Bookmark(String title, int page, List<String> sections) {
        boolean hasSections() {
            return !sections.isEmpty();
        }
    }
}
