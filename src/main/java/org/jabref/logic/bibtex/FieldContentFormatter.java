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

    private final FieldPreferences preferences;

    public FieldContentFormatter(FieldPreferences preferences) {
        Objects.requireNonNull(preferences);
        this.preferences = preferences;
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
    public String format(String fieldContent, Field field) {
        if (FieldFactory.isMultiLineField(field, preferences.getNonWrappableFields())) {
            // Keep the field as is.
            // Newlines are normalized at org.jabref.logic.exporter.BibWriter
            // Alternative: StringUtil.unifyLineBreaks(fieldContent, OS.NEWLINE)
            return fieldContent;
        }

        String result = WHITESPACE.matcher(fieldContent).replaceAll(" ");
        if (result.startsWith("{ ")) {
            // Remove starting space
            result = "{" + result.substring(2);
        }
        if (result.endsWith(" }")) {
            // Remove ending space
            result = result.substring(0, result.length() - 2) + "}";
        }
        return result;
    }

    public String format(StringBuilder fieldContent, Field field) {
        return format(fieldContent.toString(), field);
    }
}
