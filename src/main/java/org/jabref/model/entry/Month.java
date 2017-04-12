package org.jabref.model.entry;

import java.util.Optional;

import org.jabref.model.strings.StringUtil;

/**
 * Represents a month object.
 */
public enum Month {

    JANUARY("January", "jan", "01", "#jan#", 0),
    FEBRUARY("February", "feb", "02", "#feb#", 1),
    MARCH("March", "mar", "03", "#mar#", 2),
    APRIL("April", "apr", "04", "#apr#", 3),
    MAY("May", "may", "05", "#may#", 4),
    JUNE("June", "jun", "06", "#jun#", 5),
    JULY("July", "jul", "07", "#jul#", 6),
    AUGUST("August", "aug", "08", "#aug#", 7),
    SEPTEMBER("September", "sep", "09", "#sep#", 8),
    OCTOBER("October", "oct", "10", "#oct#", 9),
    NOVEMBER("November", "nov", "11", "#nov#", 10),
    DECEMBER("December", "dec", "12", "#dec#", 11);

    private final String fullName;
    private final String shortName;
    private final String twoDigitNumber;
    private final String bibtexFormat;
    private final int index;

    Month(String fullName, String shortName, String twoDigitNumber, String bibtexFormat, int index) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.twoDigitNumber = twoDigitNumber;
        this.bibtexFormat = bibtexFormat;
        this.index = index;
    }

    /**
     * Find month by one-based number.
     * If the number is not in the valid range, then an empty Optional is returned.
     *
     * @param number 1-12 is valid
     */
    public static Optional<Month> getMonthByNumber(int number) {
        return Month.getMonthByIndex(number - 1);
    }

    /**
     * Find month by zero-based index.
     * If the index is not in the valid range, then an empty Optional is returned.
     *
     * @param index 0-11 is valid
     */
    public static Optional<Month> getMonthByIndex(int index) {
        for (Month month : Month.values()) {
            if (month.index == index) {
                return Optional.of(month);
            }
        }
        return Optional.empty();
    }

    /**
     * Find month by shortName (3 letters) case insensitive.
     * If no matching month is found, then an empty Optional is returned.
     *
     * @param shortName "jan", "feb", ...
     */
    public static Optional<Month> getMonthByShortName(String shortName) {
        for (Month month : Month.values()) {
            if (month.shortName.equalsIgnoreCase(shortName)) {
                return Optional.of(month);
            }
        }
        return Optional.empty();
    }

    /**
     * This method accepts three types of months:
     * - Single and Double Digit months from 1 to 12 (01 to 12)
     * - 3 Digit BibTex strings (jan, feb, mar...) possibly with # prepended
     * - Full English Month identifiers.
     *
     * @param value the given value
     * @return the corresponding Month instance
     */
    public static Optional<Month> parse(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        // Much more liberal matching covering most known abbreviations etc.
        String testString = value.replace("#", "").trim();
        if (testString.length() > 3) {
            testString = testString.substring(0, 3);
        }
        Optional<Month> month = Month.getMonthByShortName(testString);
        if (month.isPresent()) {
            return month;
        }

        try {
            int number = Integer.parseInt(value);
            return Month.getMonthByNumber(number);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String getShortName() {
        return shortName;
    }

    public String getBibtexFormat() {
        return bibtexFormat;
    }

    public int getIndex() {
        return index;
    }

    public int getNumber() {
        return index + 1;
    }

    public String getFullName() {
        return fullName;
    }

    public String getTwoDigitNumber() {
        return twoDigitNumber;
    }
}
