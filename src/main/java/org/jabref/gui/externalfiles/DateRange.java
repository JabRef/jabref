package org.jabref.gui.externalfiles;

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

    public String getDateRange() {
        return dateRange; 
    } 
}
