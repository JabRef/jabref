package org.jabref.model.entry;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Date {

    public static final String DATE_REGEX;
    private static final DateTimeFormatter NORMALIZED_DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu[-MM][-dd]");
    private static final DateTimeFormatter SIMPLE_DATE_FORMATS;
    private static final Logger LOGGER = LoggerFactory.getLogger(Date.class);

    static {
        List<String> formatStrings = Arrays.asList(
                "uuuu-MM-dd'T'HH:mm[:ss][xxx][xx][X]",  // covers 2018-10-03T07:24:14+03:00
                "uuuu-MM-dd'T'HH:m[:ss][xxx][xx][X]",   // covers 2018-10-03T17:2
                "uuuu-MM-dd'T'H:mm[:ss][xxx][xx][X]",   // covers 2018-10-03T7:24
                "uuuu-MM-dd'T'H:m[:ss][xxx][xx][X]",    // covers 2018-10-03T7:7
                "uuuu-MM-dd'T'HH[:ss][xxx][xx][X]",     // covers 2018-10-03T07
                "uuuu-MM-dd'T'H[:ss][xxx][xx][X]",      // covers 2018-10-03T7
                "uuuu-M-d",                             // covers 2009-1-15
                "uuuu-M",                               // covers 2009-11
                "uuuu/M",                               // covers 2020/10
                "d-M-uuuu",                             // covers 15-1-2012
                "M-uuuu",                               // covers 1-2012
                "M/uuuu",                               // covers 9/2015 and 09/2015
                "M/uu",                                 // covers 9/15
                "MMMM d, uuuu",                         // covers September 1, 2015
                "MMMM, uuuu",                           // covers September, 2015
                "d.M.uuuu",                             // covers 15.1.2015
                "uuuu.M.d",                             // covers 2015.1.15
                "uuuu",                                 // covers 2015
                "MMM, uuuu",                            // covers Jan, 2020
                "uuuu.MM.d",                            // covers 2015.10.15
                "d MMMM u/d MMMM u",                    // covers 20 January 2015/20 February 2015
                "d MMMM u",                             // covers 20 January 2015
                "d MMMM u / d MMMM u"
                );

        /* TODO: The following date formats do not yet work and need to be created with tests
         *      "u G",                                  // covers 1 BC
         *       "u G / u G",                           // covers 30 BC / 5 AD
         *      "uuuu G / uuuu G",                      // covers 0030 BC / 0005 AD
         *      "uuuu-MM G / uuuu-MM G",                // covers 0030-01 BC / 0005-02 AD
         *      "u'-'",                                 // covers 2015-
         *      "u'?'",                                 // covers 2023?
         */

        SIMPLE_DATE_FORMATS = formatStrings.stream()
                                           .map(DateTimeFormatter::ofPattern)
                                           .reduce(new DateTimeFormatterBuilder(),
                                                   DateTimeFormatterBuilder::appendOptional,
                                                   (builder, formatterBuilder) -> builder.append(formatterBuilder.toFormatter()))
                                           .toFormatter(Locale.US);

        /*
         * There is also {@link org.jabref.model.entry.Date#parse(java.lang.String)}.
         * The regex of that method cannot be used as we parse single dates here and that method parses:
         * i) date ranges
         * ii) two dates separated by '/'
         * Additionally, parse method requires the reviewed String to hold only a date.
         */
        DATE_REGEX = "\\d{4}-\\d{1,2}-\\d{1,2}" + // covers YYYY-MM-DD, YYYY-M-DD, YYYY-MM-D, YYYY-M-D
                "|\\d{4}\\.\\d{1,2}\\.\\d{1,2}|" + // covers YYYY.MM.DD, YYYY.M.DD, YYYY.MM.D, YYYY.M.D
                "(January|February|March|April|May|June|July|August|September|" +
                "October|November|December) \\d{1,2}, \\d{4}"; // covers Month DD, YYYY & Month D, YYYY
    }

    private final TemporalAccessor date;
    private final TemporalAccessor endDate;

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
        endDate = null;
    }

    /**
     * Creates a Date from date and endDate.
     *
     * @param date the start date
     * @param endDate the start date
     */
    public Date(TemporalAccessor date, TemporalAccessor endDate) {
        this.date = date;
        this.endDate = endDate;
    }

    /**
     * Creates a Date from date and endDate.
     *
     * @param dateString the string to extract the date information
     * @throws DateTimeParseException if dataString is mal-formatted
     */
    public static Optional<Date> parse(String dateString) {
        Objects.requireNonNull(dateString);

        if (dateString.isEmpty()) {
            return Optional.empty();
        }

        // if dateString has range format, treat as date range
        if (dateString.matches(
               "\\d{4}/\\d{4}|" + // uuuu/uuuu
               "\\d{4}-\\d{2}/\\d{4}-\\d{2}|" + // uuuu-mm/uuuu-mm
               "\\d{4}-\\d{2}-\\d{2}/\\d{4}-\\d{2}-\\d{2}|" + // uuuu-mm-dd/uuuu-mm-dd
               "(?i)(January|February|March|April|May|June|July|August|September|October|November|December)" +
               "( |\\-)(\\d{1,4})/(January|February|March|April|May|June|July|August|September|October|November" +
               "|December)( |\\-)(\\d{1,4})(?i-)|" + // January 2015/January 2015
               "(?i)(\\d{1,2})( )(January|February|March|April|May|June|July|August|September|October|November|December)" +
               "( |\\-)(\\d{1,4})/(\\d{1,2})( )" +
               "(January|February|March|April|May|June|July|August|September|October|November|December)" +
               "( |\\-)(\\d{1,4})(?i-)" // 20 January 2015/20 January 2015
        )) {
            try {
                String[] strDates = dateString.split("/");
                TemporalAccessor parsedDate = SIMPLE_DATE_FORMATS.parse(strDates[0].strip());
                TemporalAccessor parsedEndDate = SIMPLE_DATE_FORMATS.parse(strDates[1].strip());
                return Optional.of(new Date(parsedDate, parsedEndDate));
            } catch (DateTimeParseException e) {
                LOGGER.debug("Invalid Date format for range", e);
                return Optional.empty();
            }
        } else if (dateString.matches(
              "\\d{4} / \\d{4}|" + // uuuu / uuuu
              "\\d{4}-\\d{2} / \\d{4}-\\d{2}|" + // uuuu-mm / uuuu-mm
              "\\d{4}-\\d{2}-\\d{2} / \\d{4}-\\d{2}-\\d{2}|" + // uuuu-mm-dd / uuuu-mm-dd
              "(?i)(January|February|March|April|May|June|July|August|September|October|November|December)" +
              "( |\\-)(\\d{1,4}) / (January|February|March|April|May|June|July|August|September|October|November" +
              "|December)( |\\-)(\\d{1,4})(?i-)|" + // January 2015/January 2015
              "(?i)(\\d{1,2})( )(January|February|March|April|May|June|July|August|September|October|November|December)" +
              "( |\\-)(\\d{1,4}) / (\\d{1,2})( )" +
              "(January|February|March|April|May|June|July|August|September|October|November|December)" +
              "( |\\-)(\\d{1,4})(?i-)" // 20 January 2015/20 January 2015
        )) {
            try {
                String[] strDates = dateString.split(" / ");
                TemporalAccessor parsedDate = SIMPLE_DATE_FORMATS.parse(strDates[0].strip());
                TemporalAccessor parsedEndDate = SIMPLE_DATE_FORMATS.parse(strDates[1].strip());
                return Optional.of(new Date(parsedDate, parsedEndDate));
            } catch (DateTimeParseException e) {
                LOGGER.debug("Invalid Date format range", e);
                return Optional.empty();
            }
        }
        try {
            TemporalAccessor parsedDate = SIMPLE_DATE_FORMATS.parse(dateString);
            return Optional.of(new Date(parsedDate));
        } catch (DateTimeParseException e) {
            LOGGER.debug("Invalid Date format", e);
            return Optional.empty();
        }
    }

    public static Optional<Date> parse(Optional<String> yearValue,
                                       Optional<String> monthValue,
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
                Objects.equals(getDay(), date1.getDay()) &&
                Objects.equals(get(ChronoField.HOUR_OF_DAY), date1.get(ChronoField.HOUR_OF_DAY)) &&
                Objects.equals(get(ChronoField.MINUTE_OF_HOUR), date1.get(ChronoField.MINUTE_OF_HOUR)) &&
                Objects.equals(get(ChronoField.SECOND_OF_DAY), date1.get(ChronoField.SECOND_OF_DAY)) &&
                Objects.equals(get(ChronoField.OFFSET_SECONDS), date1.get(ChronoField.OFFSET_SECONDS));
    }

    @Override
    public String toString() {
        String formattedDate = date.toString();
        if (date.isSupported(ChronoField.OFFSET_SECONDS)) {
            formattedDate = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date);
        } else if (date.isSupported(ChronoField.HOUR_OF_DAY)) {
            formattedDate = DateTimeFormatter.ISO_DATE_TIME.format(date);
        } else if (date.isSupported(ChronoField.MONTH_OF_YEAR) && date.isSupported(ChronoField.DAY_OF_MONTH)) {
            formattedDate = DateTimeFormatter.ISO_DATE.format(date);
        }
        return "Date{" +
               "date=" + formattedDate +
               '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getYear(), getMonth(), getDay(), get(ChronoField.HOUR_OF_DAY), get(ChronoField.MINUTE_OF_HOUR), get(ChronoField.OFFSET_SECONDS));
    }
}
