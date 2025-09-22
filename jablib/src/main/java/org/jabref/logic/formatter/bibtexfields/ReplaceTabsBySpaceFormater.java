package org.jabref.logic.formatter.bibtexfields;

import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

/**
 * Replaces any tab with a space
 */
public class ReplaceTabsBySpaceFormater extends Formatter {

    private static final Pattern TAB = Pattern.compile("\t+");

    @Override
    public String getName() {
        return Localization.lang("Replace tabs with space");
    }

    @Override
    public String getKey() {
        return "remove_tabs";
    }

    @Override
    public String format(@NonNull String value) {
        return TAB.matcher(value).replaceAll(" ");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Replace tabs with space in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "In \t\t CDMA";
    }
}
