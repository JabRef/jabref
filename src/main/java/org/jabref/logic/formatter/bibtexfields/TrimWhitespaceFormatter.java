package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Trim all whitespace characters(defined in java) in the string.
 */
public class TrimWhitespaceFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Trim whitespace characters");
    }

    @Override
    public String getKey() {
        return "trim_whitespace";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);
        return value.trim();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Trim all whitespace characters in the field content.");
    }

    @Override
    public String getExampleInput() {
        return "\r\n InCDMA\n\r ";
    }
}
