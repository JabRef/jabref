package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

/// Zathura, whose `-P` option opens the document at a page number counted from 1.
@NullMarked
public class ZathuraPdfReader extends AbstractExternalPdfReader {

    @Override
    public String getDisplayName() {
        return "Zathura";
    }

    @Override
    protected Set<String> getExecutableNames() {
        return Set.of("zathura");
    }

    @Override
    public List<String> getCommandLine(Path executable, Path pdfFile, int page) {
        return List.of(executable.toString(), "-P", Integer.toString(page), pdfFile.toString());
    }
}
