package org.jabref.logic.externalfiles;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;

/// Resolves which PDF an entry opens and which page of that PDF the entry starts on.
///
/// The entry's own PDF is used when it has one. Otherwise the PDF attached to the entry referenced by `crossref` is used.
/// `xref` is not followed, and only that one reference is resolved, as in [BibDatabase#getReferencedEntry].
///
/// The page is the first arabic page number written in the entry's own `pages` field, plus [PAGE_OFFSET] on the entry that owns the PDF.
/// A missing offset is `0`, so the printed page and the PDF page are the same.
/// When `pages` does not start with an arabic number, or the computed page is less than 1, the PDF is still returned and the page is empty.
@NullMarked
public final class PdfLocationResolver {

    /// How many PDF pages precede printed page 1. Stored on the entry that owns the PDF. The field name is `pageoffset`.
    public static final Field PAGE_OFFSET = FieldFactory.parseField("pageoffset");

    private static final Pattern FIRST_PAGE = Pattern.compile("\\s*([A-Za-z]?)([0-9]+|[ivxlcdmIVXLCDM]+)[A-Za-z]?");

    private static final Pattern RANGE_SEPARATOR = Pattern.compile("\\s*(?:--|\u2014|\u2013|-)\\s*");

    /// BibTeX `43+` means the pages following 43, which has no last page.
    private static final Pattern FOLLOWING_PAGES = Pattern.compile("\\s*\\+");

    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

    private PdfLocationResolver() {
    }

    public static Optional<PdfLocation> resolve(BibEntry entry, BibDatabase database) {
        return firstPdf(entry)
                .map(file -> new PdfLocation(file, pdfPage(entry, entry), entry))
                .or(() -> database.getReferencedEntry(entry)
                                  .flatMap(referenced -> firstPdf(referenced)
                                          .map(file -> new PdfLocation(file, pdfPage(entry, referenced), referenced))));
    }

    private static Optional<LinkedFile> firstPdf(BibEntry entry) {
        return entry.getFiles().stream()
                    .filter(PdfLocationResolver::isPdf)
                    .findFirst();
    }

    private static boolean isPdf(LinkedFile file) {
        if (StandardFileType.PDF.getName().equals(file.getFileType())) {
            return true;
        }
        return FileUtil.getFileExtension(file.getLink())
                       .filter("pdf"::equalsIgnoreCase)
                       .isPresent();
    }

    public static Optional<PageSpan> pdfPageSpan(BibEntry entry, BibEntry fileOwner) {
        return entry.getField(StandardField.PAGES)
                    .flatMap(PdfLocationResolver::printedSpan)
                    .flatMap(printed -> toPdfSpan(printed, pageOffset(fileOwner)));
    }

    public static int pageOffset(BibEntry fileOwner) {
        return fileOwner.getField(PAGE_OFFSET)
                        .filter(StringUtil::isNotBlank)
                        .map(String::trim)
                        .flatMap(PdfLocationResolver::parseInteger)
                        .orElse(0);
    }
    
    private static Optional<Integer> pdfPage(BibEntry entry, BibEntry fileOwner) {
        return firstPrintedPage(entry).flatMap(printed -> toPdfPage(printed, pageOffset(fileOwner)));
    }

    private static Optional<Integer> firstPrintedPage(BibEntry entry) {
        return entry.getField(StandardField.PAGES)
                    .filter(StringUtil::isNotBlank)
                    .flatMap(PdfLocationResolver::firstArabicPage);
    }

    private static Optional<PrintedSpan> printedSpan(String pages) {
        return firstArabicPage(pages).flatMap(start -> printedSpanFrom(pages, start));
    }

    private static Optional<PrintedSpan> printedSpanFrom(String pages, int start) {
        String rest = pages.substring(firstPageEnd(pages));
        if (FOLLOWING_PAGES.matcher(rest).lookingAt()) {
            return Optional.empty();
        }
        Matcher separator = RANGE_SEPARATOR.matcher(rest);
        if (!separator.lookingAt()) {
            return Optional.of(new PrintedSpan(start, start));
        }
        return firstArabicPage(rest.substring(separator.end()))
                .filter(end -> end >= start)
                .map(end -> new PrintedSpan(start, end));
    }

    private static int firstPageEnd(String pages) {
        Matcher matcher = FIRST_PAGE.matcher(pages);
        if (!matcher.lookingAt()) {
            return 0;
        }
        return matcher.end();
    }

    private static Optional<Integer> firstArabicPage(String pages) {
        Matcher matcher = FIRST_PAGE.matcher(pages);
        if (!matcher.lookingAt()) {
            return Optional.empty();
        }
        String number = matcher.group(2);
        if (!INTEGER.matcher(number).matches()) {
            return Optional.empty();
        }
        return parseInteger(number).filter(page -> page > 0);
    }

    private static Optional<PageSpan> toPdfSpan(PrintedSpan printed, int offset) {
        return toPdfPage(printed.first(), offset).flatMap(first ->
                toPdfPage(printed.last(), offset).map(last -> new PageSpan(first, last)));
    }

    private static Optional<Integer> toPdfPage(int printedPage, int offset) {
        long page = (long) printedPage + offset;
        if ((page < 1) || (page > Integer.MAX_VALUE)) {
            return Optional.empty();
        }
        return Optional.of((int) page);
    }

    private static Optional<Integer> parseInteger(String value) {
        if (!INTEGER.matcher(value).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    public record PdfLocation(LinkedFile file, Optional<Integer> page, BibEntry fileOwner) {
    }

    /// Inclusive 1-based pages in a PDF.
    public record PageSpan(int firstPage, int lastPage) {
    }

    private record PrintedSpan(int first, int last) {
    }
}
