package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.jspecify.annotations.NullMarked;

/// This formatter escapes characters so that they are suitable for HTML.
@NullMarked
public class HTMLChars implements ParamLayoutFormatter {

    private boolean keepCurlyBraces = false;

    @Override
    public void setArgument(String arg) {
        if ("keepCurlyBraces".equalsIgnoreCase(arg)) {
            this.keepCurlyBraces = true;
        }
    }

    @Override
    public String format(String inField) {
        return LatexMarkupConverter.convert(normalizedField(inField), LatexMarkupConverter.Dialect.HTML,
                                           keepCurlyBraces ? LatexMarkupConverter.GroupBraces.PRESERVE : LatexMarkupConverter.GroupBraces.DISCARD)
                                   .replace("~", "&nbsp;"); // Replace any remaining ~ with &nbsp; (non-breaking spaces)
    }

    private String normalizedField(String inField) {
        return inField.replaceAll("[\\n]{2,}", "<p>") // Replace double line breaks with <p>
                      .replace("\n", "<br>") // Replace single line breaks with <br>
                      .replace("\\$", "&dollar;") // Replace \$ with &dollar;
                      .replaceAll("\\$([^$]*)\\$", this.keepCurlyBraces ? "\\\\{$1\\\\}" : "{$1}");
    }
}
