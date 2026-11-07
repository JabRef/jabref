package org.jabref.logic.pdf.reader;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

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
