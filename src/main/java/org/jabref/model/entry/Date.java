package org.jabref.model.entry;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class Date {

    private final TemporalAccessor date;

    private static final List<SimpleDateFormat> SIMPLE_DATE_FORMATS = new ArrayList<>();

    static {
        List<String> formatStrings = Arrays.asList(
                "yyyy-M-d",
                "yyyy-M",
                "d-M-yyyy",
                "M-yyyy",
                "M/yy",
                "M/yyyy",
                "MMMM d, yyyy",
                "MMMM, yyyy",
                "d.M.yyyy",
                "yyyy.M.d", "yyyy",
                "MMM, yyyy");

        for (String formatString : formatStrings) {
            // Locale is required for parsing month names correctly. Currently this expects the month names to be in English
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(formatString, Locale.US);
            simpleDateFormat.setLenient(false);
            SIMPLE_DATE_FORMATS.add(simpleDateFormat);
        }
    }

    public static final DateTimeFormatter NORMALIZED_DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu[-MM][-dd]");

    public Date(int year, int month, int dayOfMonth) {
        this(LocalDate.of(year, month, dayOfMonth));
    }

    public Date(int year, int month) {
        this(YearMonth.of(year, month));
    }

    public Date(int year) {
        this(Year.of(year));
    }

    public Date(TemporalAccessor date) {
        this.date = date;
    }

    /**
     * Try to parse the following formats
     * - "M/y" (covers 9/15, 9/2015, and 09/2015)
     * - "MMMM (dd), yyyy" (covers September 1, 2015 and September, 2015)
     * - "yyyy-MM-dd" (covers 2009-1-15)
     * - "dd-MM-yyyy" (covers 15-1-2009)
     * - "d.M.uuuu" (covers 15.1.2015)
     * - "uuuu.M.d" (covers 2015.1.15)
     * - "MMM, uuuu" (covers Jan, 2020)
     */
    public static Optional<Date> parse(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return Optional.empty();
        }

        for (SimpleDateFormat formatter : SIMPLE_DATE_FORMATS) {
            java.util.Date date = formatter.parse(dateString, new ParsePosition(0));
            if (date != null) {
                LocalDate localDate = date
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                return Optional.of(new Date(localDate));
            }
        }

        return Optional.empty();
    }

    public static Optional<Date> parse(Optional<String> yearValue, Optional<String> monthValue,
                                       Optional<String> dayValue) {
        Optional<Year> year = yearValue.flatMap(Date::convertToInt).map(Year::of);
        Optional<Month> month = monthValue.flatMap(Month::parse);
        Optional<Integer> day = dayValue.flatMap(Date::convertToInt);

        if (year.isPresent()) {
            TemporalAccessor date;
            if (month.isPresent()) {
                if (day.isPresent()) {
                    date = LocalDate.of(year.get().getValue(), month.get().getNumber(), day.get());
                } else {
                    date = YearMonth.of(year.get().getValue(), month.get().getNumber());
                }
            } else {
                date = year.get();
            }

            return Optional.of(new Date(date));
        }

        return Optional.empty();
    }

    private static Optional<Integer> convertToInt(String value) {
        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public String getNormalized() {
        return NORMALIZED_DATE_FORMATTER.format(date);
    }

    public Optional<Integer> getYear() {
        return get(ChronoField.YEAR);
    }

    public Optional<Integer> get(ChronoField field) {
        if (date.isSupported(field)) {
            return Optional.of(date.get(field));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Month> getMonth() {
        return get(ChronoField.MONTH_OF_YEAR).flatMap(Month::getMonthByNumber);
    }

    public Optional<Integer> getDay() {
        return get(ChronoField.DAY_OF_MONTH);
    }

    public TemporalAccessor toTemporalAccessor() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        Date date1 = (Date) o;
        return Objects.equals(getYear(), date1.getYear()) &&
                Objects.equals(getMonth(), date1.getMonth()) &&
                Objects.equals(getDay(), date1.getDay());
    }

    @Override
    public String toString() {
        return "Date{" +
                "date=" + date +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(date);
    }
}
