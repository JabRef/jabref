package org.jabref.logic.layout.format;

import java.util.Locale;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.MonthUtil;

public class RisMonth implements LayoutFormatter {

    @Override
    public String format(String month) {
        if (month == null) {
            return "";
        }

        MonthUtil.Month m = MonthUtil.getMonthByShortName(month);
        if (m.isValid()) {
            return m.twoDigitNumber;
        } else {
            return month.toLowerCase(Locale.ROOT);
        }
    }

}
