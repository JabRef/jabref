/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.util;

import net.sf.jabref.util.Util;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class for everything related to months.
 */
public class MonthUtil {

    public static class Month {

        public final String fullName;
        public final String shortName;
        public final String twoDigitNumber;
        public final String bibtexFormat;
        public final int number;
        public final int index;


        public Month(String fullName, String shortName, String twoDigitNumber, String bibtexFormat, int number, int index) {
            this.fullName = fullName;
            this.shortName = shortName;
            this.twoDigitNumber = twoDigitNumber;
            this.bibtexFormat = bibtexFormat;
            this.number = number;
            this.index = index;
        }

        public boolean isValid() {
            return true;
        }
    }

    private static class UnknownMonth extends Month {

        public UnknownMonth() {
            super(null, null, null, null, 0, -1);
        }

        @Override
        public boolean isValid() {
            return false;
        }
    }


    private static final Month NULL_OBJECT = new UnknownMonth();

    private static final List<Month> months = Arrays.asList(
            new Month("January", "jan", "01", "#jan#", 1, 0),
            new Month("February", "feb", "02", "#feb#", 2, 1),
            new Month("March", "mar", "03", "#mar#", 3, 2),
            new Month("April", "apr", "04", "#apr#", 4, 3),
            new Month("May", "may", "05", "#may#", 5, 4),
            new Month("June", "jun", "06", "#jun#", 6, 5),
            new Month("July", "jul", "07", "#jul#", 7, 6),
            new Month("August", "aug", "08", "#aug#", 8, 7),
            new Month("September", "sep", "09", "#sep#", 9, 8),
            new Month("October", "oct", "10", "#oct#", 10, 9),
            new Month("November", "nov", "11", "#nov#", 11, 10),
            new Month("December", "dec", "12", "#dec#", 12, 11)
            );


    /**
     * Find month by number
     *
     * @param number 1-12 is valid
     * @return if valid number -> month.isValid() == true, else otherwise
     */
    public static Month getMonthByNumber(int number) {
        return MonthUtil.getMonthByIndex(number - 1);
    }

    /**
     * Find month by index
     *
     * @param index 0-11 is valid
     * @return if valid index -> month.isValid() == true, else otherwise
     */
    public static Month getMonthByIndex(int index) {
        for (Month month : MonthUtil.months) {
            if (month.index == index) {
                return month;
            }
        }
        return MonthUtil.NULL_OBJECT;
    }

    /**
     * Find month by shortName (3 letters) case insensitive
     *
     * @param shortName "jan", "feb", ...
     * @return if valid shortName -> month.isValid() == true, else otherwise
     */
    public static Month getMonthByShortName(String shortName) {
        for (Month month : MonthUtil.months) {
            if (month.shortName.equalsIgnoreCase(shortName)) {
                return month;
            }
        }
        return MonthUtil.NULL_OBJECT;
    }

    /**
     * This method accepts three types of months given:
     * - Single and Double Digit months from 1 to 12 (01 to 12)
     * - 3 Digit BibTex strings (jan, feb, mar...)
     * - Full English Month identifiers.
     *
     * @param value the given value
     * @return the corresponding Month instance
     */
    public static Month getMonth(String value) {
        if (value == null) {
            return MonthUtil.NULL_OBJECT;
        }

        // Much more liberal matching covering most known abbreviations etc.
        String testString = value.replace("#", "").trim();
        if (testString.length() > 3) {
            testString = testString.substring(0, 3);
        }
        Month month = MonthUtil.getMonthByShortName(testString);
        if (month.isValid()) {
            return month;
        }

        try {
            int number = Util.intValueOf(value);
            return MonthUtil.getMonthByNumber(number);
        } catch (NumberFormatException e) {
            return MonthUtil.NULL_OBJECT;
        }
    }

}
