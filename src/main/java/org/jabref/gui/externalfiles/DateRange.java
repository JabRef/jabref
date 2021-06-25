package org.jabref.gui.externalfiles;

import org.jabref.logic.l10n.Localization;

public enum DateRange {
    DAY(Localization.lang("Last day")),
    WEEK(Localization.lang("Last week")),
    MONTH(Localization.lang("Last month")),
    YEAR(Localization.lang("Last year")),
    ALL_TIME(Localization.lang("All time"));

    private final String dateRange;

    DateRange(String dateRange) { 
       this.dateRange = dateRange;
    }

    public String getDateRange() {
        return dateRange; 
    } 
}
