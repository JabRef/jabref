package org.jabref.logic.layout.format;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;

/**
 * Formatter used to signal that a formatter hasn't been found. This can be
 * used for graceful degradation if a layout uses an undefined format.
 */
public class NotFoundFormatter implements LayoutFormatter {

    private final String notFound;

    public NotFoundFormatter(String notFound) {
        this.notFound = notFound;
    }

    public String getNotFound() {
        return notFound;
    }

    @Override
    public String format(String fieldText) {
        return '[' + Localization.lang("Formatter not found: %0", notFound) + "] " + fieldText;
    }
}
