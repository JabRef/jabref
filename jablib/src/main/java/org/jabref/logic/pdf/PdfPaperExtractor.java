package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.externalfiles.PdfLocationResolver.PageSpan;
import org.jabref.logic.util.io.FileNameUniqueness;
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
        Optional<PageSpan> span = PdfLocationResolver.pdfPageSpan(entry, fileOwner);
        if (span.isEmpty()) {
            return Optional.empty();
        }

        try (PDDocument document = Loader.loadPDF(source.toFile())) {
            int pageCount = document.getNumberOfPages();
            PageSpan pages = span.orElseThrow();
            if (pages.lastPage() > pageCount) {
                return Optional.empty();
            }
            int length = pages.lastPage() - pages.firstPage() + 1;
            if (length >= pageCount) {
                return Optional.empty();
            }

            String baseName = FileUtil.getBaseName(source.getFileName().toString());
            String proposed = baseName + "-p" + pages.firstPage() + "-" + pages.lastPage() + ".pdf";
            Path target = targetDirectory.resolve(FileNameUniqueness.generateUniqueFileName(targetDirectory, proposed));
            PdfPageCopier.copy(document, pages.firstPage(), pages.lastPage(), target);
            return Optional.of(target);
        }
    }
}
