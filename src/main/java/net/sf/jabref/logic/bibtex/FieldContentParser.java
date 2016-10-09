package net.sf.jabref.logic.bibtex;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.strings.StringUtil;

/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
public class FieldContentParser {

    private final Set<String> multiLineFields;

    // 's' matches a space, tab, new line, carriage return.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");


    public FieldContentParser(FieldContentParserPreferences prefs) {
        Objects.requireNonNull(prefs);

        multiLineFields = new HashSet<>();
        // the following two are also coded in net.sf.jabref.logic.bibtex.LatexFieldFormatter.format(String, String)
        multiLineFields.add(FieldName.ABSTRACT);
        multiLineFields.add(FieldName.REVIEW);
        // the file field should not be formatted, therefore we treat it as a multi line field
        multiLineFields.addAll(prefs.getNonWrappableFields());
    }

    /**
     * Performs the reformatting
     *
     * @param fieldContent the content to format
     * @param bibtexField the name of the bibtex field
     * @return the formatted field content.
     */
    public String format(String fieldContent, String bibtexField) {

        if (multiLineFields.contains(bibtexField)) {
            // Unify line breaks
            return StringUtil.unifyLineBreaks(fieldContent, OS.NEWLINE);
        }

        return WHITESPACE.matcher(fieldContent).replaceAll(" ");
    }

    public String format(StringBuilder fieldContent, String bibtexField) {
        return format(fieldContent.toString(), bibtexField);
    }
}
