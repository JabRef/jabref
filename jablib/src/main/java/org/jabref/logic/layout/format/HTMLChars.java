package org.jabref.logic.layout.format;

import java.util.regex.Pattern;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.jspecify.annotations.NullMarked;

/// This formatter escapes characters so that they are suitable for HTML.
@NullMarked
public class HTMLChars implements ParamLayoutFormatter {

    /// This regex matches `&` that **do not begin** an HTML entity.
    ///
    /// - `&amp;` **Not Matched**
    /// - `&#34;` **Not Matched**
    /// - `&Hey` **Matched**
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&(?!(?:[a-zA-Z0-9]+|#[0-9]{1,6}|#x[0-9a-fA-F]{1,6});)");

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
        // Cannot use StringEscapeUtils#escapeHtml4 because it does not handle LaTeX characters and commands.
        // Resolve LaTeX escapes first so HTML escaping does not escape their generated entity again.
        return HTML_ENTITY_PATTERN.matcher(inField.replace("\\&", "&amp;")).replaceAll("&amp;")
                                  .replaceAll("[\\n]{2,}", "<p>") // Replace double line breaks with <p>
                                  .replace("\n", "<br>") // Replace single line breaks with <br>
                                  .replace("\\$", "&dollar;") // Replace \$ with &dollar;
                                  .replaceAll("\\$([^$]*)\\$", this.keepCurlyBraces ? "\\\\{$1\\\\}" : "{$1}");
    }
}
