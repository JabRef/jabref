package org.jabref.logic.pdf.reader;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

/// Adobe Acrobat and Acrobat Reader, the readers Adobe's PDF Open Parameters were written for.
/// `acrord32` is the Reader, `acrobat` the full product.
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
