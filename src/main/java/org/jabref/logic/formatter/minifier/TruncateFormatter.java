package org.jabref.logic.formatter.minifier;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class TruncateFormatter extends Formatter {
    private final int TRUNCATE_AFTER;
    private final String KEY;

    /**
     * The TruncateFormatter truncates a string after the given index and removes trailing whitespaces.
     *
     * @param truncateIndex truncate a string after this index.
     */
    public TruncateFormatter(final int truncateIndex) {
        TRUNCATE_AFTER = (truncateIndex >= 0) ? truncateIndex : Integer.MAX_VALUE;
        KEY = "truncate" + TRUNCATE_AFTER;
    }

    @Override
    public String getName() {
        return Localization.lang("Truncate");
    }

    @Override
    public String getKey() {
        return KEY;
    }

    /**
     * Truncates a string after the given index.
     */
    @Override
    public String format(final String input) {
        Objects.requireNonNull(input);
        final int index = Math.min(TRUNCATE_AFTER, input.length());
        return input.substring(0, index).stripTrailing();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Truncates a string after a given index.");
    }

    @Override
    public String getExampleInput() {
        return "Truncate this sentence.";
    }
}
