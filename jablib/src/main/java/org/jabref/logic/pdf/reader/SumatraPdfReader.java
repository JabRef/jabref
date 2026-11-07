package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

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
