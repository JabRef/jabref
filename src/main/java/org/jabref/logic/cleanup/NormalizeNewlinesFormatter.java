package org.jabref.logic.cleanup;

import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;

/**
 * Trim all whitespace characters (defined in java) in the string.
 */
public class NormalizeNewlinesFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Normalize newline characters");
    }

    @Override
    public String getKey() {
        return "normalize_newlines";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        boolean shouldNormalizeNewlines = !value.contains(OS.NEWLINE) && value.contains("\n");
        if (shouldNormalizeNewlines) {
            // if we don't have real new lines, but pseudo newlines, we replace them
            // On Win 8.1, this is always true for multiline fields
            return value.replace("\n", OS.NEWLINE);
        } else {
            return value;
        }
    }

    @Override
    public String getDescription() {
        return Localization.lang("Normalizes all newline characters in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "\r\n InCDMA\n ";
    }
}
