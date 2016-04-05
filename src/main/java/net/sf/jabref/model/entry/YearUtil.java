package net.sf.jabref.model.entry;

import net.sf.jabref.logic.util.strings.StringUtil;

import java.util.Calendar;

public class YearUtil {

    private static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);


    /**
     * Will convert a two digit year using the following scheme (describe at
     * http://www.filemaker.com/help/02-Adding%20and%20view18.html):
     * <p/>
     * If a two digit year is encountered they are matched against the last 69
     * years and future 30 years.
     * <p/>
     * For instance if it is the year 1992 then entering 23 is taken to be 1923
     * but if you enter 23 in 1993 then it will evaluate to 2023.
     *
     * @param year The year to convert to 4 digits.
     * @return
     */
    public static String toFourDigitYear(String year) {
        return YearUtil.toFourDigitYear(year, YearUtil.CURRENT_YEAR);
    }

    /**
     * Will convert a two digit year using the following scheme (describe at
     * http://www.filemaker.com/help/02-Adding%20and%20view18.html):
     * <p/>
     * If a two digit year is encountered they are matched against the last 69
     * years and future 30 years.
     * <p/>
     * For instance if it is the year 1992 then entering 23 is taken to be 1923
     * but if you enter 23 in 1993 then it will evaluate to 2023.
     *
     * @param year The year to convert to 4 digits.
     * @return
     */
    static String toFourDigitYear(String year, int thisYear) {
        if ((year == null) || (year.length() != 2)) {
            return year;
        }

        Integer yearNumber = StringUtil.intValueOfWithNull(year);
        if(yearNumber == null) {
            return year;
        }

        return String.valueOf(new Year(thisYear).toFourDigitYear(yearNumber));
    }

    public static int toFourDigitYearWithInts(String year) {
        return YearUtil.toFourDigitYearWithInts(year, YearUtil.CURRENT_YEAR);
    }

    private static int toFourDigitYearWithInts(String year, int thisYear) {
        if ((year == null) || (year.length() != 2)) {
            return 0;
        }

        Integer yearNumber = StringUtil.intValueOfWithNull(year);
        if(yearNumber == null) {
            return 0;
        }

        return new Year(thisYear).toFourDigitYear(yearNumber);
    }


    private static class Year {

        private final int year;
        private final int century;
        private final int yearShort;


        public Year(int year) {
            this.year = year;
            this.yearShort = this.year % 100;
            this.century = (this.year / 100) * 100;
        }

        public int toFourDigitYear(int relativeYear) {
            if (relativeYear == yearShort) {
                return this.year;
            }
            // 20 , 90
            // 99 > 30
            if ((((relativeYear + 100) - yearShort) % 100) > 30) {
                if (relativeYear < yearShort) {
                    return century + relativeYear;
                } else {
                    return (century - 100) + relativeYear;
                }
            } else {
                if (relativeYear < yearShort) {
                    return century + 100 + relativeYear;
                } else {
                    return century + relativeYear;
                }
            }
        }
    }
}
