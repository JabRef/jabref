package org.jabref.logic.externalfiles;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
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

    /// First page token of a `pages` value: an optional one-letter affix, then an arabic or roman number.
    private static final Pattern FIRST_PAGE = Pattern.compile("\\s*([A-Za-z]?)([0-9]+|[ivxlcdmIVXLCDM]+)[A-Za-z]?");

    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");

    private PdfLocationResolver() {
    }

    /// @param entry    the entry being opened
    /// @param database the library that contains `entry` and, when used, its cross-referenced entry
    /// @return empty when neither entry has a PDF
    public static Optional<PdfLocation> resolve(BibEntry entry, BibDatabase database) {
        Optional<LinkedFile> ownPdf = firstPdf(entry);
        if (ownPdf.isPresent()) {
            return Optional.of(new PdfLocation(ownPdf.get(), pdfPage(entry, entry), entry));
        }

        return database.getReferencedEntry(entry)
                       .flatMap(referenced -> firstPdf(referenced)
                               .map(file -> new PdfLocation(file, pdfPage(entry, referenced), referenced)));
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

    /// Page in the PDF, or empty when the entry's `pages` field does not give an arabic start page.
    private static Optional<Integer> pdfPage(BibEntry entry, BibEntry fileOwner) {
        return firstPrintedPage(entry).flatMap(printed -> toPdfPage(printed, pageOffset(fileOwner)));
    }

    private static Optional<Integer> firstPrintedPage(BibEntry entry) {
        return entry.getField(StandardField.PAGES)
                    .map(String::trim)
                    .filter(pages -> !pages.isEmpty())
                    .flatMap(PdfLocationResolver::firstArabicPage);
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

    /// A missing or non-numeric value is 0, which leaves the printed page unchanged.
    private static int pageOffset(BibEntry fileOwner) {
        return fileOwner.getField(PAGE_OFFSET)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .flatMap(PdfLocationResolver::parseInteger)
                        .orElse(0);
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

    /// @param file      the PDF to open
    /// @param page      1-based page inside `file`, empty when no page can be worked out
    /// @param fileOwner the entry whose `file` field holds `file`
    public record PdfLocation(LinkedFile file, Optional<Integer> page, BibEntry fileOwner) {
    }
}
