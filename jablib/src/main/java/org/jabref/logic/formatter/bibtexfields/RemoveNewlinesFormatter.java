package org.jabref.logic.formatter.bibtexfields;

import java.util.regex.Pattern;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

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
    public String format(@NonNull String value) {
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
