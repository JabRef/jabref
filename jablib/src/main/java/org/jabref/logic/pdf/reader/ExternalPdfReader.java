package org.jabref.logic.pdf.reader;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NullMarked;

/// An external PDF reader that can be told to open a document at a given page.
///
/// Implementations only assemble a command line; starting the process is the caller's task.
@NullMarked
public interface ExternalPdfReader {

    /// The product name, used in user-facing messages. Not localized.
    String getDisplayName();

    /// @param executable the application configured for the PDF file type
    /// @return whether `executable` is this reader
    boolean matches(Path executable);

    /// @param page 1-based page number, as returned by [org.jabref.model.pdf.FileAnnotation#getPage]
    /// @return the complete command line, `executable` first
    List<String> getCommandLine(Path executable, Path pdfFile, int page);
}
