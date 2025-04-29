package org.jabref.logic.layout.format;

import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.Month;

public class RisMonth implements LayoutFormatter {

    @Override
    public String format(String month) {
        if (month == null) {
            return "";
        }

        Optional<Month> parsedMonth = Month.getMonthByShortName(month);
        return parsedMonth.map(Month::getTwoDigitNumber).orElse(month.toLowerCase(Locale.ROOT));
    }
}
