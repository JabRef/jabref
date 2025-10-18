package org.jabref.model.groups;

/**
 * Defines the granularity level for date-based automatic grouping.
 */
public enum DateGranularity {
    /** Group by year only (e.g., 2024) */
    YEAR,
    /** Group by year and month (e.g., 2024-01) */
    MONTH,
    /** Group by full date (e.g., 2024-01-15) */
    FULL_DATE
}
