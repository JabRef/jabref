package org.jabref.logic.formatter.bibtexfields;

import java.util.regex.Pattern;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import org.jspecify.annotations.NonNull;

/**
 * Replaces two subsequent whitespaces (and tabs) to one space in case of single-line fields. In case of multine fields,
 * the field content is kept as is.
 * <p>
 * Due to the distinction between single line and multiline fields, this formatter does not implement the interface {@link org.jabref.logic.cleanup.Formatter}.
 */
public class NormalizeWhitespaceFormatter {

    // 's' matches a space, tab, new line, carriage return.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final FieldPreferences preferences;

    public NormalizeWhitespaceFormatter(@NonNull FieldPreferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Performs the reformatting of a field content. Note that "field content" is either with enclosing {}.
     * When outputting something which is using strings, the parts of the plain string are passed (without enclosing {}).
     * For instance, for <code>#kopp# and #breit#"</code>, <code> and </code> is passed.
     * Also depends on the caller whether strings have been resolved.
     *
     * @param fieldContent the content to format.
     * @param field        the name of the bibtex field
     * @return the formatted field content.
     */
    public String format(String fieldContent, Field field) {
        if (FieldFactory.isMultiLineField(field, preferences.getNonWrappableFields())) {
            // In general, keep the field as is.
            // Newlines are normalized at org.jabref.logic.exporter.BibWriter
            // Alternative: StringUtil.unifyLineBreaks(fieldContent, OS.NEWLINE)
            return fieldContent;
        }

        // Replace multiple whitespaces by one. We need to keep the leading and trailing whitespace to enable constructs such as "#kopp# and #breit#"
        return WHITESPACE.matcher(fieldContent).replaceAll(" ");
    }

    /**
     * Performs the reformatting of a field content. Note that "field content" is understood as
     * the value in BibTeX's key/value pairs of content. For instance, <code>{author}</code> is passed as
     * content. This allows for things like <code>jan { - } feb</code> to be passed.
     *
     * @param fieldContent the content to format.
     * @param field        the name of the bibtex field
     * @return the formatted field content.
     */
    public String format(StringBuilder fieldContent, Field field) {
        return format(fieldContent.toString(), field);
    }
}
