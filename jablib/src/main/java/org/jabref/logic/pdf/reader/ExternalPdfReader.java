package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ExternalPdfReader {

    String getDisplayName();

    boolean matches(Path executable);

    /// `page` is 1-based, as in [org.jabref.model.pdf.FileAnnotation#getPage].
    List<String> getCommandLine(Path executable, Path pdfFile, int page);
}
