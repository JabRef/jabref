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

import org.jspecify.annotations.NonNull;
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
                "MMMM  uuuu",                           // covers September 2015
                "d.M.uuuu",                             // covers 15.1.2015
                "uuuu.M.d",                             // covers 2015.1.15
                "uuuu",                                 // covers 2015
                "MMM, uuuu",                            // covers Jan, 2020
                "MMM. uuuu",                            // covers Oct. 2020
                "MMM uuuu",                             // covers Jan 2020
                "uuuu.MM.d",                            // covers 2015.10.15
                "d MMMM u/d MMMM u",                    // covers 20 January 2015/20 February 2015
                "d MMMM u",                             // covers 20 January 2015
                "d MMMM u / d MMMM u",
                "u'-'",                                 // covers 2015-
                "u'?'",                                 // covers 2023?
                "u G",                                  // covers 1 BC and 1 AD
                "uuuu G",                               // covers 0030 BC and 0005 AD
                "u G/u G",                              // covers 30 BC/5 AD
                "uuuu G/uuuu G",                        // covers 0030 BC/0005 AD
                "uuuu-MM G/uuuu-MM G"                   // covers 0030-01 BC/0005-02 AD
        );

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
    private final Season season;

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
        season = null;
    }

    /**
     * Creates a Date from date and endDate.
     *
     * @param date    the start date
     * @param endDate the start date
     */
    public Date(TemporalAccessor date, TemporalAccessor endDate) {
        this.date = date;
        this.endDate = endDate;
        this.season = null;
    }

    public Date(TemporalAccessor date, Season season) {
        this.date = date;
        this.season = season;
        this.endDate = null;
    }

    /**
     * Creates a Date from date and endDate.
     *
     * @param dateString the string to extract the date information
     * @throws DateTimeParseException if dataString is mal-formatted
     */
    public static Optional<Date> parse(@NonNull String dateString) {
        dateString = dateString.strip();

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
                LOGGER.warn("Invalid Date format for range", e);
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
                LOGGER.warn("Invalid Date format range", e);
                return Optional.empty();
            }
        } else if (dateString.matches(
                "\\d{1,4} BC/\\d{1,4} AD|" + // 30 BC/5 AD and 0030 BC/0005 AD
                        "\\d{1,4} BC/\\d{1,4} BC|" + // 30 BC/10 BC and 0030 BC/0010 BC
                        "\\d{1,4} AD/\\d{1,4} AD|" + // 5 AD/10 AD and 0005 AD/0010 AD
                        "\\d{1,4}-\\d{1,2} BC/\\d{1,4}-\\d{1,2} AD|" + // 5 AD/10 AD and 0005 AD/0010 AD
                        "\\d{1,4}-\\d{1,2} BC/\\d{1,4}-\\d{1,2} BC|" + // 5 AD/10 AD and 0005 AD/0010 AD
                        "\\d{1,4}-\\d{1,2} AD/\\d{1,4}-\\d{1,2} AD" // 5 AD/10 AD and 0005 AD/0010 AD
        )) {
            try {
                String[] strDates = dateString.split("/");
                TemporalAccessor parsedDate = parseDateWithEraIndicator(strDates[0]);
                TemporalAccessor parsedEndDate = parseDateWithEraIndicator(strDates[1]);
                return Optional.of(new Date(parsedDate, parsedEndDate));
            } catch (DateTimeParseException e) {
                LOGGER.warn("Invalid Date format range", e);
                return Optional.empty();
            }
        } else if (dateString.matches(
                "\\d{1,4} BC / \\d{1,4} AD|" + // 30 BC / 5 AD and 0030 BC / 0005 AD
                        "\\d{1,4} BC / \\d{1,4} BC|" + // 30 BC / 10 BC and 0030 BC / 0010 BC
                        "\\d{1,4} AD / \\d{1,4} AD|" + // 5 AD / 10 AD and 0005 AD / 0010 AD
                        "\\d{1,4}-\\d{1,2} BC / \\d{1,4}-\\d{1,2} AD|" + // 5 AD/10 AD and 0005 AD/0010 AD
                        "\\d{1,4}-\\d{1,2} BC / \\d{1,4}-\\d{1,2} BC|" + // 5 AD/10 AD and 0005 AD/0010 AD
                        "\\d{1,4}-\\d{1,2} AD / \\d{1,4}-\\d{1,2} AD" // 5 AD/10 AD and 0005 AD/0010 AD
        )) {
            try {
                String[] strDates = dateString.split(" / ");
                TemporalAccessor parsedDate = parseDateWithEraIndicator(strDates[0]);
                TemporalAccessor parsedEndDate = parseDateWithEraIndicator(strDates[1]);
                return Optional.of(new Date(parsedDate, parsedEndDate));
            } catch (DateTimeParseException e) {
                LOGGER.warn("Invalid Date format range", e);
                return Optional.empty();
            }
        }

        // if dateString is single year
        if (dateString.matches("\\d{4}-|\\d{4}\\?")) {
            try {
                String year = dateString.substring(0, dateString.length() - 1);
                TemporalAccessor parsedDate = SIMPLE_DATE_FORMATS.parse(year);
                return Optional.of(new Date(parsedDate));
            } catch (DateTimeParseException e) {
                LOGGER.debug("Invalid Date format", e);
                return Optional.empty();
            }
        }

        // handle the new date formats with era indicators
        if (dateString.matches(
                "\\d{1,4} BC|" + // covers 1 BC
                        "\\d{1,4} AD|" + // covers 1 BC
                        "\\d{1,4}-\\d{1,2} BC|" +  // covers 0030-01 BC
                        "\\d{1,4}-\\d{1,2} AD" // covers 0005-01 AD
        )) {
            try {
                // Parse the date with era indicator
                TemporalAccessor date = parseDateWithEraIndicator(dateString);
                return Optional.of(new Date(date));
            } catch (DateTimeParseException e) {
                LOGGER.warn("Invalid Date format with era indicator", e);
                return Optional.empty();
            }
        }
        // handle date whose month is represented as a season.
        if (dateString.matches(
                "^(\\d{1,4})-(\\d{1,2})$" // covers 2025-21
        )) {
            try {
                // parse the date with season
                Optional<Date> optional = parseDateWithSeason(dateString);
                if (optional.isPresent()) {
                    return optional;
                }
                // else, just pass
            } catch (DateTimeParseException e) {
                // neither month nor season.
                LOGGER.debug("Invalid Date format", e);
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

    /**
     * Create a date with a string with era indicator.
     *
     * @param dateString the string which contain era indicator to extract the date information
     * @return the date information with TemporalAccessor type
     */
    private static TemporalAccessor parseDateWithEraIndicator(String dateString) {
        String yearString = dateString.strip().substring(0, dateString.length() - 2);

        String[] parts = yearString.split("-");
        int year = Integer.parseInt(parts[0].strip());

        if (dateString.endsWith("BC")) {
            year = 1 - year;
        }
        if (parts.length > 1) {
            int month = Integer.parseInt(parts[1].strip());
            return YearMonth.of(year, month);
        }
        return Year.of(year);
    }

    /**
     * Create a date whose month is represented as a season.
     *
     * @param dateString the string which contain season to extract the date information
     * @return the date information with TemporalAccessor type
     */
    private static Optional<Date> parseDateWithSeason(String dateString) {
        String[] parts = dateString.split("-");
        int monthOrSeason = Integer.parseInt(parts[1].strip());
        // Is month, don't parse it here.
        if (monthOrSeason >= 1 && monthOrSeason <= 12) {
            return Optional.empty();
        }
        // check month is season
        Optional<Season> optional = Season.getSeasonByNumber(monthOrSeason);
        if (optional.isPresent()) {
            int year = Integer.parseInt(parts[0].strip());
            // use month as season
            return Optional.of(new Date(Year.of(year), optional.get()));
        }
        throw new DateTimeParseException("Invalid Date format for season", dateString, parts[0].length());
    }

    public static String sanitizeIncompleteRange(String dateString) {
        if (dateString == null) {
            return null;
        }

        String trimmed = dateString.trim();

        if (trimmed.endsWith("/") && trimmed.matches(".+\\d{4}/")) {
            LOGGER.debug("Sanitizing incomplete range (trailing slash): {}", trimmed);
            return trimmed.substring(0, trimmed.length() - 1).trim();
        }

        if (trimmed.startsWith("/") && trimmed.matches("/\\d{4}.+")) {
            LOGGER.debug("Sanitizing incomplete range (leading slash): {}", trimmed);
            return trimmed.substring(1).trim();
        }

        return dateString;
    }

    private boolean isRange() {
        return endDate != null;
    }

    public String getNormalized() {
        if (isRange()) {
            String normalizedStartDate = NORMALIZED_DATE_FORMATTER.format(date);
            String normalizedEndDate = NORMALIZED_DATE_FORMATTER.format(endDate);
            return normalizedStartDate + "/" + normalizedEndDate;
        }
        return NORMALIZED_DATE_FORMATTER.format(date);
    }

    public Optional<TemporalAccessor> getEndDate() {
        return Optional.ofNullable(endDate);
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

    public Optional<Season> getSeason() {
        return Optional.ofNullable(season);
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
                Objects.equals(getSeason(), date1.getSeason()) &&
                Objects.equals(getDay(), date1.getDay()) &&
                Objects.equals(get(ChronoField.HOUR_OF_DAY), date1.get(ChronoField.HOUR_OF_DAY)) &&
                Objects.equals(get(ChronoField.MINUTE_OF_HOUR), date1.get(ChronoField.MINUTE_OF_HOUR)) &&
                Objects.equals(get(ChronoField.SECOND_OF_DAY), date1.get(ChronoField.SECOND_OF_DAY)) &&
                Objects.equals(get(ChronoField.OFFSET_SECONDS), date1.get(ChronoField.OFFSET_SECONDS));
    }

    @Override
    public String toString() {
        String formattedDate = date.toString();
        // If there is a season, then only the year and month fields will have values,
        // and the month corresponds to the season.
        // Here is no need to check the second, hour, and month, day fields.
        if (season != null) {
            // The Date standard library does not have any API for handling seasons,
            // so here is no compact form.
            return "Date{" +
                    "date=" + formattedDate + ", " +
                    "season=" + season.getName() +
                    '}';
        }
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

    public Optional<Integer> getYear() {
        return get(ChronoField.YEAR);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getYear(), getMonth(), getSeason(), getDay(), get(ChronoField.HOUR_OF_DAY), get(ChronoField.MINUTE_OF_HOUR), get(ChronoField.OFFSET_SECONDS));
    }
}
