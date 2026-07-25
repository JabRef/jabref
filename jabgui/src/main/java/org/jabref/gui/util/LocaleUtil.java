package org.jabref.gui.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

public final class LocaleUtil {
    private LocaleUtil() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static String formatInstant(@Nullable Instant instant) {
        if (instant == null) {
            return "";
        }

        return instant
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                         .withLocale(Locale.getDefault()));
    }
}
