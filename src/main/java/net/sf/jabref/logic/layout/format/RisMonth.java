package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.model.entry.MonthUtil;

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
            return month.toLowerCase();
        }
    }

}
