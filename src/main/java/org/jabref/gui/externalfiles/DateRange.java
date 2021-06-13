package org.jabref.gui.externalfiles;

public enum DateRange {
    DAY("Last day"),
    WEEK("Last week"),
    MONTH("Last month"),
    YEAR("Last year"),
    ALL_TIME("All time");

    private final String dateRange;

    DateRange(String dateRange) { 
       this.dateRange = dateRange;
    }

    public String getDateRange() { return dateRange; } 
}


