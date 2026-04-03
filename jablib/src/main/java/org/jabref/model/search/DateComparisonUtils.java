package org.jabref.model.search;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateComparisonUtils {
    public enum DateOperator {
        GREATER_THAN,
        GREATER_OR_EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL
    }

    /**
     * Compares an entry's date against a user's search query.
     */
    public static boolean evaluateDateRange(String entryDate, String searchDate, DateOperator operator) {
        if (entryDate == null || searchDate == null || entryDate.trim().isEmpty() || searchDate.trim().isEmpty()) {
            return false;
        }

        LocalDate entry = parseToLocalDate(entryDate.trim());
        LocalDate search = parseToLocalDate(searchDate.trim());

        // If either date failed to parse into a valid format, we cannot compare them
        if (entry == null || search == null) {
            return false;
        }

        switch (operator) {
            case GREATER_THAN:
                return entry.isAfter(search);
            case GREATER_OR_EQUAL:
                return entry.isEqual(search) || entry.isAfter(search);
            case LESS_THAN:
                return entry.isBefore(search);
            case LESS_OR_EQUAL:
                return entry.isEqual(search) || entry.isBefore(search);
            default:
                return false;
        }
    }

    /**
     * Helper method to convert BibTeX dates into a standard LocalDate.
     * "2024"       -> 2024-01-01
     * "2024-05"    -> 2024-05-01
     * "2024-05-17" -> 2024-05-17
     */
    private static LocalDate parseToLocalDate(String dateStr) {
        try {
            if (dateStr.length() == 4) {
                return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
            } else if (dateStr.length() == 7 && dateStr.contains("-")) {
                String[] parts = dateStr.split("-");
                return LocalDate.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), 1);
            } else {
                return LocalDate.parse(dateStr);
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            return null;
        }
    }
}
