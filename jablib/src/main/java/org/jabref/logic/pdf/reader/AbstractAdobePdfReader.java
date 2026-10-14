package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NullMarked;

/// A reader that takes its page through the `/A` switch defined by Adobe's
/// *Parameters for Opening PDF Files*, the de-facto convention among the Windows readers.
@NullMarked
public abstract class AbstractAdobePdfReader extends AbstractExternalPdfReader {

    @Override
    public List<String> getCommandLine(Path executable, Path pdfFile, int page) {
        return List.of(executable.toString(), "/A", "page=" + page, pdfFile.toString());
    }
}
