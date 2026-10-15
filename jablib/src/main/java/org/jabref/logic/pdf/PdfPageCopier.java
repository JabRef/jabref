package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;

/// Copies a 1-based inclusive page range out of an open PDF. The source document is left unchanged.
@NullMarked
final class PdfPageCopier {

    private PdfPageCopier() {
    }

    static void copy(PDDocument source, int firstPage, int lastPage, Path target) throws IOException {
        try (PDDocument copy = new PDDocument()) {
            for (int page = firstPage; page <= lastPage; page++) {
                copy.importPage(source.getPage(page - 1));
            }
            copy.save(target.toFile());
        }
    }
}
