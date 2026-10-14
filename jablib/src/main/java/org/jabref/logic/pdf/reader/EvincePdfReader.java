package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

/// Evince, which offers both `-i`/`--page-index` for the physical page and `-p`/`--page-label` for
/// the printed label. Annotations carry the physical page, so `-i` is the matching option.
@NullMarked
public class EvincePdfReader extends AbstractExternalPdfReader {

    @Override
    public String getDisplayName() {
        return "Evince";
    }

    @Override
    protected Set<String> getExecutableNames() {
        return Set.of("evince");
    }

    @Override
    public List<String> getCommandLine(Path executable, Path pdfFile, int page) {
        return List.of(executable.toString(), "-i", Integer.toString(page), pdfFile.toString());
    }
}
