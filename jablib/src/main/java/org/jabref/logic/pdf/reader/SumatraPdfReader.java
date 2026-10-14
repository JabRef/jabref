package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

/// SumatraPDF understands the Adobe `/A "page=N"` parameters only from version 3.5 on, so the
/// native `-page` argument is used instead. `-reuse-instance` makes the jump reach an already open
/// window regardless of the user's `ReuseInstance` setting, which is what turns a second click into
/// a jump rather than a new window.
///
/// <https://www.sumatrapdfreader.org/docs/Command-line-arguments>
@NullMarked
public class SumatraPdfReader extends AbstractExternalPdfReader {

    @Override
    public String getDisplayName() {
        return "SumatraPDF";
    }

    @Override
    protected Set<String> getExecutableNames() {
        return Set.of("sumatrapdf");
    }

    @Override
    public List<String> getCommandLine(Path executable, Path pdfFile, int page) {
        return List.of(executable.toString(), "-reuse-instance", "-page", Integer.toString(page), pdfFile.toString());
    }
}
