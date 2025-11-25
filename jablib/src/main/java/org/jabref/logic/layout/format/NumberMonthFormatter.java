package org.jabref.logic.layout.format;

import java.util.Optional;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Month;

public class NumberMonthFormatter implements LayoutFormatter {

    @Override
    public String format(String fieldText) {
        Optional<Month> month = Month.parse(fieldText);
        return month.map(Month::getTwoDigitNumber).orElse("01");
    }
}
