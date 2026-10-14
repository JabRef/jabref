package org.jabref.logic.pdf.reader;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

/// Foxit PDF Reader, Foxit Reader and Foxit PhantomPDF, which follow the Adobe `/A` convention.
///
/// <https://resource-cms.foxit.com/foxit-pdf-editor-for-windows/command-lines>
@NullMarked
public class FoxitPdfReader extends AbstractAdobePdfReader {

    @Override
    public String getDisplayName() {
        return "Foxit PDF Reader";
    }

    @Override
    protected Set<String> getExecutableNames() {
        return Set.of("foxitpdfreader", "foxitreader", "foxitphantompdf");
    }
}
