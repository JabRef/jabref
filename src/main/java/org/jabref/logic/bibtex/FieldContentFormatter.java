package org.jabref.logic.bibtex;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.jabref.logic.util.OS;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
public class FieldContentFormatter {

    // 's' matches a space, tab, new line, carriage return.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final Set<Field> multiLineFields;

    public FieldContentFormatter(FieldContentFormatterPreferences preferences) {
        Objects.requireNonNull(preferences);

        multiLineFields = new HashSet<>();
        // the following two are also coded in org.jabref.logic.bibtex.LatexFieldFormatter.format(String, String)
        multiLineFields.add(StandardField.ABSTRACT);
        multiLineFields.add(StandardField.COMMENT);
        multiLineFields.add(StandardField.REVIEW);
        // the file field should not be formatted, therefore we treat it as a multi line field
        multiLineFields.addAll(preferences.getNonWrappableFields());
    }

    /**
     * Performs the reformatting
     *
     * @param fieldContent the content to format
     * @param field        the name of the bibtex field
     * @return the formatted field content.
     */
    public String format(String fieldContent, Field field) {
        if (multiLineFields.contains(field)) {
            return StringUtil.unifyLineBreaks(fieldContent, OS.NEWLINE);
        }

        return WHITESPACE.matcher(fieldContent).replaceAll(" ");
    }

    public String format(StringBuilder fieldContent, Field field) {
        return format(fieldContent.toString(), field);
    }
}
