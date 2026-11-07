package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

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
