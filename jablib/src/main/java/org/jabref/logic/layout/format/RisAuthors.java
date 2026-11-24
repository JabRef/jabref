package org.jabref.logic.layout.format;

import java.util.stream.Collectors;

import org.jabref.logic.layout.ParamLayoutFormatter;
import org.jabref.logic.os.OS;
import org.jabref.model.entry.AuthorList;

import org.jspecify.annotations.Nullable;

public class RisAuthors implements ParamLayoutFormatter {

    private String arg = "";

    /// Formats each author as a separate RIS line, e.g. `AU  - Smith, John`.
    ///
    /// The authors are parsed before any LaTeX conversion so that brace-protected
    /// organization names such as `{Research and Development Institute}` stay a single author.
    @Override
    public String format(@Nullable String fieldText) {
        if (fieldText == null) {
            return "";
        }
        return AuthorList.parse(normalizeWhitespaceInsideBraces(fieldText))
                         .latexFree()
                         .getAuthors()
                         .stream()
                         .map(author -> arg + "  - " + author.getFamilyGiven(false))
                         .collect(Collectors.joining(OS.NEWLINE));
    }

    /// Collapses line breaks and repeated whitespace inside braces to a single space.
    ///
    /// Whitespace outside braces is kept, because `AuthorListParser` treats line breaks and
    /// multiple spaces between names as author separators.
    private static String normalizeWhitespaceInsideBraces(String fieldText) {
        StringBuilder result = new StringBuilder(fieldText.length());
        int braceLevel = 0;
        boolean previousWasWhitespace = false;
        for (char c : fieldText.toCharArray()) {
            if (c == '{') {
                braceLevel++;
            } else if ((c == '}') && (braceLevel > 0)) {
                braceLevel--;
            }
            if ((braceLevel > 0) && Character.isWhitespace(c)) {
                if (!previousWasWhitespace) {
                    result.append(' ');
                }
                previousWasWhitespace = true;
            } else {
                result.append(c);
                previousWasWhitespace = false;
            }
        }
        return result.toString();
    }

    @Override
    public void setArgument(String arg) {
        this.arg = arg;
    }
}
