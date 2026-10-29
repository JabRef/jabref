package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.externalfiles.PdfLocationResolver.PageSpan;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;

// [impl->req~logic.pdf.extract-paper-page-range~1]
@NullMarked
public final class PdfPaperExtractor {

    private PdfPaperExtractor() {
    }

    /// @param fileOwner the entry whose `pageoffset` numbers the pages of `source`
    /// @return the new PDF, or empty when `entry` does not name a strict sub-range of `source`
    public static Optional<Path> extractPaper(Path source, BibEntry entry, BibEntry fileOwner, Path targetDirectory) throws IOException {
        Optional<PageSpan> pages = pagesToExtract(source, entry, fileOwner);
        if (pages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(extractPages(source, pages.orElseThrow(), targetDirectory));
    }

    /// @param fileOwner the entry whose `pageoffset` numbers the pages of `source`
    /// @return the PDF pages that `entry` names, or empty when they are not a strict sub-range of `source`
    public static Optional<PageSpan> pagesToExtract(Path source, BibEntry entry, BibEntry fileOwner) throws IOException {
        Optional<PageSpan> span = PdfLocationResolver.pdfPageSpan(entry, fileOwner);
        if (span.isEmpty()) {
            return Optional.empty();
        }
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            int pageCount = document.getNumberOfPages();
            return span.filter(pages -> (pages.lastPage() <= pageCount) && (pages.lastPage() - pages.firstPage() + 1 < pageCount));
        }
    }

    /// @return the new PDF holding `pages` of `source`
    public static Path extractPages(Path source, PageSpan pages, Path targetDirectory) throws IOException {
        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            String baseName = FileUtil.getBaseName(source.getFileName().toString());
            return PdfPageCopier.copyToNewFile(document, pages.firstPage(), pages.lastPage(), targetDirectory, baseName + "-p" + pages.firstPage() + "-" + pages.lastPage() + ".pdf");
        }
    }
}
