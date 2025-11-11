package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Removes all line breaks in the string.
 */
public class RemoveNewlinesFormatter extends Formatter {
    private static final Pattern LINEBREAKS = Pattern.compile("\\R");

    @Override
    public String getName() {
        return Localization.lang("Remove line breaks");
    }

    @Override
    public String getKey() {
        return "remove_newlines";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        value = LINEBREAKS.matcher(value).replaceAll(" ");
        return value.trim();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes all line breaks in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "In \n CDMA";
    }
}
