package org.jabref.logic.formatter.bibtexfields;

import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

public class RemoveDigitsFormatter extends Formatter {

    private static final Pattern DIGITS = Pattern.compile("[ ]\\d+");

    @Override
    public String getName() {
        return Localization.lang("Remove digits");
    }

    @Override
    public String getKey() {
        return "remove_digits";
    }

    @Override
    public String format(@NonNull String value) {
        return DIGITS.matcher(value).replaceAll("");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Removes digits.");
    }

    @Override
    public String getExampleInput() {
        return "In 012 CDMA";
    }
}
