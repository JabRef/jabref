package org.jabref.logic.layout.format;

import java.util.Optional;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Month;

/**
 * Convert the month name into the corresponding number and return 01 by default
 */
public class NumberMonthFormatter implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        Optional<Month> month = Month.parse(fieldText);
        return month.map(Month::getTwoDigitNumber).orElse("01");
    }
}
