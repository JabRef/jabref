package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;
import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Removes all hyphenated line breaks in the string.
 */
public class RemoveHyphenatedNewlinesFormatter extends Formatter {
    private static final Pattern HYPHENATED_WORDS = Pattern.compile("-\\R");

    @Override
    public String getName() {
        return Localization.lang("Remove hyphenated line breaks");
    }

    @Override
    public String getKey() {
        return "remove_hyphenated_newlines";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        value = HYPHENATED_WORDS.matcher(value).replaceAll("");
        return value.trim();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes all hyphenated line breaks in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "Gimme shel-\nter";
    }
}
