package org.jabref.logic.pdf.reader;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

/// PDF-XChange Viewer (`pdfxcview`) and Editor (`pdfxedit`), which follow the Adobe `/A` convention.
///
/// The preference "Restore the last used view of documents" takes precedence over the requested
/// page, so a user who sees no jump has to clear it.
///
/// <https://help.pdf-xchange.com/pdfxt9/command-line-options_ed.html>
@NullMarked
public class PdfXChangePdfReader extends AbstractAdobePdfReader {

    @Override
    public String getDisplayName() {
        return "PDF-XChange";
    }

    @Override
    protected Set<String> getExecutableNames() {
        return Set.of("pdfxcview", "pdfxedit");
    }
}
