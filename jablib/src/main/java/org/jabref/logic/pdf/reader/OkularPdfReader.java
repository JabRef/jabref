package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class OkularPdfReader extends AbstractExternalPdfReader {

    @Override
    public String getDisplayName() {
        return "Okular";
    }

    @Override
    protected Set<String> getExecutableNames() {
        return Set.of("okular");
    }

    @Override
    public List<String> getCommandLine(Path executable, Path pdfFile, int page) {
        return List.of(executable.toString(), "-p", Integer.toString(page), pdfFile.toString());
    }
}
