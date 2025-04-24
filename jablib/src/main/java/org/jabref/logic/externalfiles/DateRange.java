package org.jabref.logic.externalfiles;

import org.jabref.logic.l10n.Localization;

public enum DateRange {
    ALL_TIME(Localization.lang("All time")),
    YEAR(Localization.lang("Last year")),
    MONTH(Localization.lang("Last month")),
    WEEK(Localization.lang("Last week")),
    DAY(Localization.lang("Last day"));

    private final String dateRange;

    DateRange(String dateRange) {
       this.dateRange = dateRange;
    }

    public static DateRange parse(String name) {
        try {
            return DateRange.valueOf(name);
        } catch (IllegalArgumentException e) {
            return ALL_TIME;
        }
    }

    public String getDateRange() {
        return dateRange;
    }
}
