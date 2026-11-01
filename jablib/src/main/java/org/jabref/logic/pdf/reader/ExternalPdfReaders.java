package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// The readers JabRef knows how to open at a given page.
@NullMarked
public class ExternalPdfReaders {

    private static final List<ExternalPdfReader> READERS = List.of(
            new AcrobatPdfReader(),
            new EvincePdfReader(),
            new FoxitPdfReader(),
            new OkularPdfReader(),
            new PdfXChangePdfReader(),
            new SumatraPdfReader(),
            new ZathuraPdfReader());

    private ExternalPdfReaders() {
    }

    public static Optional<ExternalPdfReader> getReader(Path executable) {
        return READERS.stream()
                      .filter(reader -> reader.matches(executable))
                      .findFirst();
    }
}
