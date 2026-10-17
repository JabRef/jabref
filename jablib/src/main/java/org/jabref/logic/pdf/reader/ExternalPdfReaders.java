package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// The readers JabRef knows how to open at a given page.
///
/// This is an allowlist on purpose: an unknown program would read `/A` and `page=5` as file names
/// and answer with an error dialog or a blank document, so arguments are only ever added after a
/// positive match.
@NullMarked
public class ExternalPdfReaders {

    /// Alphabetical; the order carries no meaning, as no two readers claim the same executable name.
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

    /// @param executable the application configured for the PDF file type
    /// @return empty if no known reader matches, meaning no page argument may be passed
    public static Optional<ExternalPdfReader> getReader(Path executable) {
        return READERS.stream()
                      .filter(reader -> reader.matches(executable))
                      .findFirst();
    }
}
