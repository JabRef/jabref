package org.jabref.logic.bibtex;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
public class FieldContentFormatter {

    // 's' matches a space, tab, new line, carriage return.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private static Pattern RTRIM = Pattern.compile("\\s+$");

    private final FieldPreferences preferences;

    public FieldContentFormatter(FieldPreferences preferences) {
        Objects.requireNonNull(preferences);
        this.preferences = preferences;
    }

    /**
     * Performs the reformatting of a field content. Note that "field content" is either with enclosing {}.
     * When outputting something which is using strings, the parts of the plain string are passed (without enclosing {}).
     * For instance, for <code>#kopp# and #breit#"</code>, <code> and </code> is passed.
     * Also depends on the caller whether strings have been resolved.
     *
     * Mutliline fields are just right trimmed.
     *
     * @param fieldContent the content to format.
     * @param field        the name of the bibtex field
     * @return the formatted field content.
     */
    public String format(String fieldContent, Field field) {
        if (FieldFactory.isMultiLineField(field, preferences.getNonWrappableFields())) {
            // In general, keep the field as is.
            // However, we need to right trim the field for a nice display in the .bib file
            // Newlines are normalized at org.jabref.logic.exporter.BibWriter
            // Alternative: StringUtil.unifyLineBreaks(fieldContent, OS.NEWLINE)
            return RTRIM.matcher(fieldContent).replaceAll("");
        }

        // Replace multiple whitespaces by one. We need to keep the leading and trailing whitespace to enable constructs such as "#kopp# and #breit#"
        String result = WHITESPACE.matcher(fieldContent).replaceAll(" ");
        return result;
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
