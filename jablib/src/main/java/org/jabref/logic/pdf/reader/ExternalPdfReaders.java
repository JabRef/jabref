package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;

import org.jspecify.annotations.NullMarked;

/// The readers JabRef knows how to open at a given page.
@NullMarked
public class ExternalPdfReaders {

    private static final IntFunction<List<String>> ADOBE_PAGE_ARGUMENTS = page -> List.of("/A", "page=" + page);
    private static final List<ExternalPdfReader> READERS = List.of(
            new ExternalPdfReader("Adobe Acrobat", Set.of("acrord32", "acrobat"), ADOBE_PAGE_ARGUMENTS),
            new ExternalPdfReader("Evince", Set.of("evince"), page -> List.of("-i", Integer.toString(page))),
            new ExternalPdfReader("Foxit PDF Reader", Set.of("foxitpdfreader", "foxitreader", "foxitphantompdf"), ADOBE_PAGE_ARGUMENTS),
            new ExternalPdfReader("Okular", Set.of("okular"), page -> List.of("-p", Integer.toString(page))),
            new ExternalPdfReader("PDF-XChange", Set.of("pdfxcview", "pdfxedit"), ADOBE_PAGE_ARGUMENTS),
            new ExternalPdfReader("SumatraPDF", Set.of("sumatrapdf"), page -> List.of("-reuse-instance", "-page", Integer.toString(page))),
            new ExternalPdfReader("Zathura", Set.of("zathura"), page -> List.of("-P", Integer.toString(page))));

    private ExternalPdfReaders() {
    }

    public static Optional<ExternalPdfReader> getReader(Path executable) {
        return READERS.stream()
                      .filter(reader -> reader.matches(executable))
                      .findFirst();
    }
}
