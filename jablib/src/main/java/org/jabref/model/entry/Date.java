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
                "uuuu-MM-dd'T'HH:mm[:ss][xxx][xx][X]",
                "uuuu-MM-dd'T'HH:m[:ss][xxx][xx][X]",
                "uuuu-MM-dd'T'H:mm[:ss][xxx][xx][X]",
                "uuuu-MM-dd'T'H:m[:ss][xxx][xx][X]",
                "uuuu-MM-dd'T'HH[:ss][xxx][xx][X]",
                "uuuu-MM-dd'T'H[:ss][xxx][xx][X]",
                "uuuu-M-d",
                "uuuu-M",
                "uuuu/M",
                "d-M-uuuu",
                "M-uuuu",
                "M/uuuu",
                "M/uu",
                "MMMM d, uuuu",
                "MMMM, uuuu",
                "MMMM uuuu",
                "d.M.uuuu",
                "uuuu.M.d",
                "uuuu",
                "MMM, uuuu",
                "MMM. uuuu",
                "MMM uuuu",
                "uuuu.MM.d",
                "d MMMM u/d MMMM u",
                "d MMMM u",
                "d MMMM u / d MMMM u",
                "u'-'",
                "u'?'",
                "u G",
                "uuuu G",
                "u G/u G",
                "uuuu G/uuuu G",
                "uuuu-MM G/uuuu-MM G"
        );

        SIMPLE_DATE_FORMATS = formatStrings.stream()
                                           .map(DateTimeFormatter::ofPattern)
                                           .reduce(new DateTimeFormatterBuilder(),
                                                   DateTimeFormatterBuilder::appendOptional,
                                                   (builder, formatterBuilder) -> builder.append(formatterBuilder.toFormatter()))
                                           .toFormatter(Locale.US);

        DATE_REGEX = "\\d{4}-\\d{1,2}-\\d{1,2}"
                + "|\\d{4}\\.\\d{1,2}\\.\\d{1,2}|"
                + "(January|February|March|April|May|June|July|August|September|"
                + "October|November|December) \\d{1,2}, \\d{4}";
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
        this.endDate = null;
        this.season = null;
    }

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

    public static Optional<Date> parse(@NonNull String dateString) {
        dateString = dateString.strip();

        if (dateString.isEmpty()) {
            return Optional.empty();
        }

        // Parse date ranges
        if (dateString.matches(
                "\\d{4}/\\d{4}|"
                        + "\\d{4}-\\d{2}/\\d{4}-\\d{2}|"
                        + "\\d{4}-\\d{2}-\\d{2}/\\d{4}-\\d{2}-\\d{2}")) {
            try {
                String[] strDates = dateString.split("/");
                TemporalAccessor parsedDate = SIMPLE_DATE_FORMATS.parse(strDates[0].strip());
                TemporalAccessor parsedEndDate = SIMPLE_DATE_FORMATS.parse(strDates[1].strip());
                return Optional.of(new Date(parsedDate, parsedEndDate));
            } catch (DateTimeParseException e) {
                LOGGER.warn("Invalid date format for range", e);
                return Optional.empty();
            }
        }

        try {
            TemporalAccessor parsedDate = SIMPLE_DATE_FORMATS.parse(dateString);
            return Optional.of(new Date(parsedDate));
        } catch (DateTimeParseException e) {
            LOGGER.debug("Invalid date format", e);
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

    private boolean isRange() {
        return endDate != null;
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

        return Objects.equals(getYear(), date1.getYear())
                && Objects.equals(getMonth(), date1.getMonth())
                && Objects.equals(getSeason(), date1.getSeason())
                && Objects.equals(getDay(), date1.getDay())
                && Objects.equals(get(ChronoField.HOUR_OF_DAY), date1.get(ChronoField.HOUR_OF_DAY))
                && Objects.equals(get(ChronoField.MINUTE_OF_HOUR), date1.get(ChronoField.MINUTE_OF_HOUR))
                && Objects.equals(get(ChronoField.SECOND_OF_DAY), date1.get(ChronoField.SECOND_OF_DAY))
                && Objects.equals(get(ChronoField.OFFSET_SECONDS), date1.get(ChronoField.OFFSET_SECONDS));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getYear(), getMonth(), getSeason(), getDay(),
                get(ChronoField.HOUR_OF_DAY), get(ChronoField.MINUTE_OF_HOUR), get(ChronoField.OFFSET_SECONDS));
    }

    @Override
    public String toString() {
        String formattedDate = date.toString();
        if (season != null) {
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
        return "Date{" + "date=" + formattedDate + '}';
    }
}
