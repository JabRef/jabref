package net.sf.jabref.model.entry;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Date {

    private final TemporalAccessor date;

    public Date(int day, MonthUtil.Month month, int year) {
        this.date = LocalDate.of(year, month.number, day);
    }

    public Date(TemporalAccessor date) {
        this.date = date;
    }

    public static Optional<Date> parse(String day, String month, String year) {
        try {
            int dayParsed = Integer.parseInt(day);
            MonthUtil.Month monthParsed = MonthUtil.getMonth(month);
            int yearParsed = Integer.parseInt(year);
            return Optional.of(new Date(dayParsed, monthParsed, yearParsed));
        } catch (NumberFormatException | DateTimeException exception) {
            return Optional.empty();
        }
    }

    /**
     * Try to parse the following formats "M/y" (covers 9/15, 9/2015, and 09/2015) "MMMM (dd), yyyy" (covers September
     * 1, 2015 and September, 2015) "yyyy-MM-dd" (covers 2009-1-15) "d.M.uuuu" (covers 15.1.2015) "uuuu.M.d" (covers
     * 2015.1.15) The code is essentially taken from http://stackoverflow.com/questions/4024544/how-to-parse-dates-in-multiple-formats-using-simpledateformat.
     */
    public static Optional<Date> parse(String dateString) {
        List<String> formatStrings =
                Arrays.asList("uuuu-M-d", "uuuu-M", "M/uu", "M/uuuu", "MMMM d, uuuu", "MMMM, uuuu", "d.M.uuuu", "uuuu.M.d");
        for (String formatString : formatStrings) {
            try {
                TemporalAccessor parsed = DateTimeFormatter.ofPattern(formatString).parse(dateString);
                return Optional.of(new Date(parsed));
            } catch (DateTimeParseException ignored) {
                // Ignored
            }
        }

        return Optional.empty();
    }

    /**
     * Formats the date to a string of the form yyyy-mm-dd or yyyy-mm.
     */
    public String getNormalized() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("uuuu-MM[-dd]");
        return dateFormatter.format(date);
    }
}
