package org.jabref.logic.formatter.bibtexfields;

import java.util.regex.Pattern;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;

/**
 * Finds any occurrence of consecutive spaces and replaces it with a single space
 */
public class RemoveRedundantSpacesFormatter extends Formatter {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile(" {2,}");

    @Override
    public String getName() {
        return Localization.lang("Remove redundant spaces");
    }

    @Override
    public String getKey() {
        return "remove_redundant_spaces";
    }

    @Override
    public String format(@NonNull String value) {
        return MULTIPLE_SPACES.matcher(value).replaceAll(" ");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Replaces consecutive spaces with a single space in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "In   CDMA";
    }
}
