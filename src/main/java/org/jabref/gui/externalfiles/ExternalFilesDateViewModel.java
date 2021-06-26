package org.jabref.gui.externalfiles;

public class ExternalFilesDateViewModel {

    private final String description;
    private final DateRange dateRange;

    ExternalFilesDateViewModel(DateRange dateRange) {
        this.description = dateRange.getDateRange();
        this.dateRange = dateRange;
    }

    public String getDescription() {
        return this.description;
    }

    public DateRange getDateRanges() {
        return this.dateRange;
    }
}
