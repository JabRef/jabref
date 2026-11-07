package org.jabref.logic.pdf.reader;

import java.util.Set;

import org.jspecify.annotations.NullMarked;

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
