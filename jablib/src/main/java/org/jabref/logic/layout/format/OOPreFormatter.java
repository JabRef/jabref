package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.jspecify.annotations.NullMarked;

/// This formatter preprocesses JabRef fields before they are run through the layout of the bibliography style. It handles translation of LaTeX italic/bold commands into HTML tags.
@NullMarked
public class OOPreFormatter implements LayoutFormatter {

    @Override
    public String format(String field) {
        String normalizedField = field.replaceAll("&|\\\\&", "&") // Replace & and \& with &
                                      .replace("\\$", "&dollar;") // Replace \$ with &dollar;
                                      .replaceAll("\\$([^$]*)\\$", "\\{$1\\}"); // Replace $...$ with {...} to simplify conversion

        return LatexMarkupConverter.convert(normalizedField, LatexMarkupConverter.Dialect.WRITER, LatexMarkupConverter.GroupBraces.DISCARD)
                                   .replace("&dollar;", "$"); // Replace &dollar; with $
    }
}
