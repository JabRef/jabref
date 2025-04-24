package org.jabref.model.entry;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SeasonTest {

    @ParameterizedTest
    @MethodSource({"parseName", "parseNumber", "parseNameGerman", "parseSpecialCases"})
    void parseCorrectly(Optional<Season> expected, String input) {
        assertEquals(expected, Season.parse(input));
    }

    private static Stream<Arguments> parseName() {
        return Stream.of(
                arguments(Optional.of(Season.SPRING), "spring"),
                arguments(Optional.of(Season.SUMMER), "summer"),
                arguments(Optional.of(Season.AUTUMN), "autumn"),
                arguments(Optional.of(Season.WINTER), "winter"),
                arguments(Optional.of(Season.SPRING), "Spring"),
                arguments(Optional.of(Season.SUMMER), "Summer"),
                arguments(Optional.of(Season.AUTUMN), "Autumn"),
                arguments(Optional.of(Season.WINTER), "Winter")
        );
    }

    private static Stream<Arguments> parseNumber() {
        return Stream.of(
                arguments(Optional.of(Season.SPRING), "21"),
                arguments(Optional.of(Season.SUMMER), "22"),
                arguments(Optional.of(Season.AUTUMN), "23"),
                arguments(Optional.of(Season.WINTER), "24")
        );
    }

    private static Stream<Arguments> parseNameGerman() {
        return Stream.of(
                arguments(Optional.of(Season.SPRING), "frühling"),
                arguments(Optional.of(Season.SUMMER), "sommer"),
                arguments(Optional.of(Season.AUTUMN), "herbst"),
                // Since 'winter' is the same as in English, it will not be passed to 'parseGermanSeason'
                arguments(Optional.of(Season.WINTER), "winter")
        );
    }

    private static Stream<Arguments> parseSpecialCases() {
        return Stream.of(
                arguments(Optional.empty(), ";lkjasdf"),
                arguments(Optional.empty(), ".24"),
                arguments(Optional.empty(), "2.4"),
                arguments(Optional.empty(), "24."),
                arguments(Optional.empty(), "")
        );
    }

    @ParameterizedTest
    @MethodSource("getSeasonByNumberTest")
    void getSeasonByNumberTest(Optional<Season> expected, int input) {
        assertEquals(expected, Season.getSeasonByNumber(input));
    }

    private static Stream<Arguments> getSeasonByNumberTest() {
        return Stream.of(
                arguments(Optional.empty(), 13),
                arguments(Optional.empty(), 20),
                arguments(Optional.of(Season.SPRING), 21),
                arguments(Optional.of(Season.SUMMER), 22),
                arguments(Optional.of(Season.AUTUMN), 23),
                arguments(Optional.of(Season.WINTER), 24),
                arguments(Optional.empty(), 25),
                arguments(Optional.empty(), 26)
        );
    }

    @ParameterizedTest
    @MethodSource("parseGermanSeasonTest")
    void parseGermanSeasonTest(Optional<Season> expected, String input) {
        assertEquals(expected, Season.parseGermanSeason(input));
    }

    private static Stream<Arguments> parseGermanSeasonTest() {
        return Stream.of(
                arguments(Optional.empty(), "spring"),
                arguments(Optional.empty(), "1234"),
                arguments(Optional.empty(), ""),
                arguments(Optional.of(Season.SPRING), "frühling"),
                arguments(Optional.of(Season.SUMMER), "sommer"),
                arguments(Optional.of(Season.AUTUMN), "herbst"),
                arguments(Optional.of(Season.WINTER), "winter")
        );
    }

    @ParameterizedTest
    @MethodSource("getNumberTest")
    void getNumberTest(int expected, Season season) {
        assertEquals(expected, season.getNumber());
    }

    private static Stream<Arguments> getNumberTest() {
        return Stream.of(
                arguments(21, Season.SPRING),
                arguments(22, Season.SUMMER),
                arguments(23, Season.AUTUMN),
                arguments(24, Season.WINTER)
        );
    }

    @ParameterizedTest
    @MethodSource("getNameTest")
    void getNameTest(String expected, Season season) {
        assertEquals(expected, season.getName());
    }

    private static Stream<Arguments> getNameTest() {
        return Stream.of(
                arguments("spring", Season.SPRING),
                arguments("summer", Season.SUMMER),
                arguments("autumn", Season.AUTUMN),
                arguments("winter", Season.WINTER)
        );
    }
}
