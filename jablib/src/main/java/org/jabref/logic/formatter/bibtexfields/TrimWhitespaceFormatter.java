package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trim all whitespace characters (as defined in Java) in the beginning and at the end of the string.
 */
public class TrimWhitespaceFormatter extends Formatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrimWhitespaceFormatter.class);

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
        String result = value.trim();
        LOGGER.trace("Formatted '{}' to '{}'", value, result);
        return result;
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
