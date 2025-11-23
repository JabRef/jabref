package org.jabref.model.entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DateRangeUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateRangeUtil.class);

    public static String sanitizeIncompleteRange(String dateString) {
        if (dateString == null) {
            return null;
        }

        String trimmed = dateString.trim();

        if (trimmed.endsWith("/") && trimmed.matches(".+\\d{4}/")) {
            LOGGER.debug("Sanitizing incomplete range (trailing slash): {}", trimmed);
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }

        if (trimmed.startsWith("/") && trimmed.matches("/\\d{4}.+")) {
            LOGGER.debug("Sanitizing incomplete range (leading slash): {}", trimmed);
            return trimmed.substring(1).trim();
        }

        return dateString;
    }
}
