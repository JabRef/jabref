package org.jabref.logic.pdf.reader;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class AcrobatPdfReader extends AbstractAdobePdfReader {

    @Override
    public String getDisplayName() {
        return "Adobe Acrobat";
    }

    @Override
    protected Set<String> getExecutableNames() {
        return Set.of("acrord32", "acrobat");
    }
}
