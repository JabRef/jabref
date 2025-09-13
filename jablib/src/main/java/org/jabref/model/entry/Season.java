package org.jabref.model.entry;

import java.util.Optional;

import org.jabref.model.strings.StringUtil;

/**
 * Represents a Season of the Year.
 */
public enum Season {

    SPRING("spring", 21),
    SUMMER("summer", 22),
    AUTUMN("autumn", 23),
    WINTER("winter", 24);

    private final String name;
    private final int number;

    Season(String name, int number) {
        this.name = name;
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
     * Find season by name case-insensitive.
     * If no matching season is found, then an empty Optional is returned.
     *
     * @param name spring, summer, autumn, winter
     */
    public static Optional<Season> getSeasonByName(String name) {
        for (Season season : Season.values()) {
            if (season.name.equalsIgnoreCase(name)) {
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

        String testString = value;
        Optional<Season> season = Season.getSeasonByName(testString);
        if (season.isPresent()) {
            return season;
        }

        season = Season.parseGermanSeason(testString);
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
    static Optional<Season> parseGermanSeason(String value) {
        value = value.toLowerCase();
        return switch (value) {
            case "frühling" ->
                    Optional.of(SPRING);
            case "sommer" ->
                    Optional.of(SUMMER);
            case "herbst" ->
                    Optional.of(AUTUMN);
            case "winter" ->
                    Optional.of(WINTER);
            default ->
                    Optional.empty();
        };
    }

    /**
     * Returns the number of the Season: SPRING -> 21, SUMMER -> 22  etc.
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
    public String getName() {
        return name;
    }
}
