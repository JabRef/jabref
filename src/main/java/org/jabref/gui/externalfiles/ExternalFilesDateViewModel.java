package org.jabref.gui.externalfiles;


import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.gui.externalfiles.DateRange;

public class ExternalFilesDateViewModel {

    private final String description;
    private final String dateRange;

    ExternalFilesDateViewModel(String dateRange) {
        this.description = dateRange;
        this.dateRange = dateRange;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDateRanges() {
        return this.dateRange;
    }
}


