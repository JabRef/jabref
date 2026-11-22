package org.jabref.logic.layout.format;

import java.util.stream.Collectors;

import org.jabref.logic.layout.ParamLayoutFormatter;
import org.jabref.logic.os.OS;
import org.jabref.model.entry.AuthorList;

public class RisAuthors implements ParamLayoutFormatter {

    private String arg = "";

    /// Formats each author as a separate RIS line, e.g. `AU  - Smith, John`.
    ///
    /// The authors are parsed before any LaTeX conversion so that brace-protected
    /// organization names such as `{Research and Development Institute}` stay a single author.
    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return "";
        }
        // BibTeX treats line breaks and repeated whitespace as a single space
        String normalizedFieldText = fieldText.replaceAll("\\s+", " ").trim();
        return AuthorList.parse(normalizedFieldText)
                         .latexFree()
                         .getAuthors()
                         .stream()
                         .map(author -> arg + "  - " + author.getFamilyGiven(false))
                         .collect(Collectors.joining(OS.NEWLINE));
    }

    @Override
    public void setArgument(String arg) {
        this.arg = arg;
    }
}
