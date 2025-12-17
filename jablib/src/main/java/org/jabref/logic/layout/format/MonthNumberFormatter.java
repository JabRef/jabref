package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Month;

/**
 * Convert the month name into the corresponding number and return 01 by default
 */
public class MonthNumberFormatter implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        return Month.parse(fieldText).map(Month::getTwoDigitNumber).orElse("01");
    }
}
