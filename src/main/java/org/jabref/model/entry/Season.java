package org.jabref.model.entry;

import java.util.Optional;

import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.model.strings.StringUtil;

/**
 * Represents a Season of the Year.
 */
public enum Season {

    SPRING("spring", "spr", 21),
    SUMMER("summer", "sum", 22),
    AUTUMN("autumn", "aut", 23),
    WINTER("winter", "win", 24);

    private final String fullName;
    private final String shortName;
    private final int number;

    Season(String fullName, String shortName, int number) {
        this.fullName = fullName;
        this.shortName = shortName;
        this.number = number;
    }

    /**
     * Find season by number.
     * If the number is not in the valid range, then an empty Optional is returned.
     *
     * @param number 21-24 is valid
     */
    public static Optional<Season> getSeasonByNumber(int number) {
        for (Season season : Season.values()) {
            if (season.number == number) {
                return Optional.of(season);
            }
        }
        return Optional.empty();
    }

    /**
     * Find season by shortName (3 letters) case-insensitive.
     * If no matching season is found, then an empty Optional is returned.
     *
     * @param shortName spr, sum, aut, win
     */
    public static Optional<Season> getSeasonByShortName(String shortName) {
        for (Season season : Season.values()) {
            if (season.shortName.equalsIgnoreCase(shortName)) {
                return Optional.of(season);
            }
        }
        return Optional.empty();
    }

    /**
     * Find season by shortName (3 letters) case-insensitive.
     * If no matching season is found, then an empty Optional is returned.
     *
     * @param fullName spring, summer, autumn, winter
     */
    public static Optional<Season> getSeasonByFullName(String fullName) {
        for (Season season : Season.values()) {
            if (season.fullName.equalsIgnoreCase(fullName)) {
                return Optional.of(season);
            }
        }
        return Optional.empty();
    }

    /**
     * This method accepts three types of seasons:
     * - Double Digit seasons from 21 to 24
     * - Full English Season identifiers.
     *
     * @param value the given value
     * @return the corresponding Season instance
     */
    public static Optional<Season> parse(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        // Much more liberal matching covering most known abbreviations etc.
        String testString = value;
        Optional<Season> season = Season.getSeasonByFullName(testString);
        if (season.isPresent()) {
            return season;
        }

        season = Season.parseGermanShortSeason(testString);
        if (season.isPresent()) {
            return season;
        }

        try {
            int number = Integer.parseInt(value);
            return Season.getSeasonByNumber(number);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Parses a season having the string in German standard form
     *
     * @param value a String that represents a season in German form
     * @return the corresponding season instance, empty if input is not in German
     * form
     */
    static Optional<Season> parseGermanShortSeason(String value) {
        value = value.toLowerCase();
        return switch (value) {
            case "fruehling", "frühling" -> Season.getSeasonByNumber(1);
            case "sommer" -> Season.getSeasonByNumber(2);
            case "herbst" -> Season.getSeasonByNumber(3);
            case "winter" -> Season.getSeasonByNumber(4);
            default -> Optional.empty();
        };
    }

    /**
     * Returns the name of a Season in a short (3-letter) format. (spr, sum, aut, win)
     *
     * @return 3-letter identifier for a Season
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Returns the season in JabRef format. The format is the short 3-digit name surrounded by a '#' (FieldWriter.BIBTEX_STRING_START_END_SYMBOL).
     * Example: #jan#, #feb#, etc.
     * <p>
     * See <a href="https://github.com/JabRef/jabref/issues/263#issuecomment-151246595">Issue 263</a> for a discussion on that thing.
     * This seems to be an <em>invalid</em> format in terms of plain BiBTeX, but a <em>valid</em> format in the case of JabRef.
     * The documentation is available at the <a href="https://docs.jabref.org/fields/strings">Strings help</a> of JabRef.
     *
     * @return Season in JabRef format
     */
    public String getJabRefFormat() {
        return (FieldWriter.BIBTEX_STRING_START_END_SYMBOL + "%s" + FieldWriter.BIBTEX_STRING_START_END_SYMBOL).formatted(shortName);
    }

    /**
     * Returns the number of the Season: 21 -> Spring, 22 -> Summer etc.
     *
     * @return number of the season in the Year
     */
    public int getNumber() {
        return number;
    }

    /**
     * Returns the name of the long in unabbreviated english.
     *
     * @return Season
     */
    public String getFullName() {
        return fullName;
    }
}
