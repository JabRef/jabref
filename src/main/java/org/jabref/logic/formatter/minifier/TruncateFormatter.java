package org.jabref.logic.formatter.minifier;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

public class TruncateFormatter extends Formatter {
    public static final String KEY = "truncate";
    private static Integer truncateAfter;

    /**
     * The TruncateFormatter truncates a string after the given index.
     *
     * @param truncateIndex truncate a string after this index.
     */
    public TruncateFormatter(int truncateIndex) {
        truncateAfter = truncateIndex;
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
     * Truncates a string after a given index.
     */
    @Override
    public String format(final String input) {
        final int truncateIndex = Math.min(truncateAfter, input.length());
        return input.substring(0, truncateIndex).stripTrailing();
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
