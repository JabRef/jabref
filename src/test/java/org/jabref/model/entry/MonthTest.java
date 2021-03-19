package org.jabref.model.entry;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class MonthTest {

    @ParameterizedTest
    @MethodSource({"parseShortName", "parseBibtexName", "parseFullName", "parseTwoDigitNumber", "parseNumber", "parseShortNameGerman", "parseFullNameGerman", "parseShortNameGermanLowercase", "parseSpecialCases"})
    void parseCorrectly(Optional<Month> expected, String input) {
        assertEquals(expected, Month.parse(input));
    }

    private static Stream<Arguments> parseShortName() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "jan"),
                arguments(Optional.of(Month.FEBRUARY), "feb"),
                arguments(Optional.of(Month.MARCH), "mar"),
                arguments(Optional.of(Month.APRIL), "apr"),
                arguments(Optional.of(Month.MAY), "may"),
                arguments(Optional.of(Month.JUNE), "jun"),
                arguments(Optional.of(Month.JULY), "jul"),
                arguments(Optional.of(Month.AUGUST), "aug"),
                arguments(Optional.of(Month.SEPTEMBER), "sep"),
                arguments(Optional.of(Month.OCTOBER), "oct"),
                arguments(Optional.of(Month.NOVEMBER), "nov"),
                arguments(Optional.of(Month.DECEMBER), "dec")
        );
    }

    private static Stream<Arguments> parseBibtexName() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "#jan#"),
                arguments(Optional.of(Month.FEBRUARY), "#feb#"),
                arguments(Optional.of(Month.MARCH), "#mar#"),
                arguments(Optional.of(Month.APRIL), "#apr#"),
                arguments(Optional.of(Month.MAY), "#may#"),
                arguments(Optional.of(Month.JUNE), "#jun#"),
                arguments(Optional.of(Month.JULY), "#jul#"),
                arguments(Optional.of(Month.AUGUST), "#aug#"),
                arguments(Optional.of(Month.SEPTEMBER), "#sep#"),
                arguments(Optional.of(Month.OCTOBER), "#oct#"),
                arguments(Optional.of(Month.NOVEMBER), "#nov#"),
                arguments(Optional.of(Month.DECEMBER), "#dec#")
        );
    }

    private static Stream<Arguments> parseFullName() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "January"),
                arguments(Optional.of(Month.FEBRUARY), "February"),
                arguments(Optional.of(Month.MARCH), "March"),
                arguments(Optional.of(Month.APRIL), "April"),
                arguments(Optional.of(Month.MAY), "May"),
                arguments(Optional.of(Month.JUNE), "June"),
                arguments(Optional.of(Month.JULY), "July"),
                arguments(Optional.of(Month.AUGUST), "August"),
                arguments(Optional.of(Month.SEPTEMBER), "September"),
                arguments(Optional.of(Month.OCTOBER), "October"),
                arguments(Optional.of(Month.NOVEMBER), "November"),
                arguments(Optional.of(Month.DECEMBER), "December")
        );
    }

    private static Stream<Arguments> parseTwoDigitNumber() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "01"),
                arguments(Optional.of(Month.FEBRUARY), "02"),
                arguments(Optional.of(Month.MARCH), "03"),
                arguments(Optional.of(Month.APRIL), "04"),
                arguments(Optional.of(Month.MAY), "05"),
                arguments(Optional.of(Month.JUNE), "06"),
                arguments(Optional.of(Month.JULY), "07"),
                arguments(Optional.of(Month.AUGUST), "08"),
                arguments(Optional.of(Month.SEPTEMBER), "09"),
                arguments(Optional.of(Month.OCTOBER), "10"),
                arguments(Optional.of(Month.NOVEMBER), "11"),
                arguments(Optional.of(Month.DECEMBER), "12")
        );
    }

    private static Stream<Arguments> parseNumber() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "1"),
                arguments(Optional.of(Month.FEBRUARY), "2"),
                arguments(Optional.of(Month.MARCH), "3"),
                arguments(Optional.of(Month.APRIL), "4"),
                arguments(Optional.of(Month.MAY), "5"),
                arguments(Optional.of(Month.JUNE), "6"),
                arguments(Optional.of(Month.JULY), "7"),
                arguments(Optional.of(Month.AUGUST), "8"),
                arguments(Optional.of(Month.SEPTEMBER), "9"),
                arguments(Optional.of(Month.OCTOBER), "10"),
                arguments(Optional.of(Month.NOVEMBER), "11"),
                arguments(Optional.of(Month.DECEMBER), "12")
        );
    }

    private static Stream<Arguments> parseShortNameGerman() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "Jan"),
                arguments(Optional.of(Month.FEBRUARY), "Feb"),
                arguments(Optional.of(Month.MARCH), "Mär"),
                arguments(Optional.of(Month.MARCH), "Mae"),
                arguments(Optional.of(Month.APRIL), "Apr"),
                arguments(Optional.of(Month.MAY), "Mai"),
                arguments(Optional.of(Month.JUNE), "Jun"),
                arguments(Optional.of(Month.JULY), "Jul"),
                arguments(Optional.of(Month.AUGUST), "Aug"),
                arguments(Optional.of(Month.SEPTEMBER), "Sep"),
                arguments(Optional.of(Month.OCTOBER), "Okt"),
                arguments(Optional.of(Month.NOVEMBER), "Nov"),
                arguments(Optional.of(Month.DECEMBER), "Dez")
        );
    }

    private static Stream<Arguments> parseFullNameGerman() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "Januar"),
                arguments(Optional.of(Month.FEBRUARY), "Februar"),
                arguments(Optional.of(Month.MARCH), "März"),
                arguments(Optional.of(Month.MARCH), "Maerz"),
                arguments(Optional.of(Month.APRIL), "April"),
                arguments(Optional.of(Month.MAY), "Mai"),
                arguments(Optional.of(Month.JUNE), "Juni"),
                arguments(Optional.of(Month.JULY), "Juli"),
                arguments(Optional.of(Month.AUGUST), "August"),
                arguments(Optional.of(Month.SEPTEMBER), "September"),
                arguments(Optional.of(Month.OCTOBER), "Oktober"),
                arguments(Optional.of(Month.NOVEMBER), "November"),
                arguments(Optional.of(Month.DECEMBER), "Dezember")
        );
    }

    private static Stream<Arguments> parseShortNameGermanLowercase() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "jan"),
                arguments(Optional.of(Month.FEBRUARY), "feb"),
                arguments(Optional.of(Month.MARCH), "mär"),
                arguments(Optional.of(Month.MARCH), "mae"),
                arguments(Optional.of(Month.APRIL), "apr"),
                arguments(Optional.of(Month.MAY), "mai"),
                arguments(Optional.of(Month.JUNE), "jun"),
                arguments(Optional.of(Month.JULY), "jul"),
                arguments(Optional.of(Month.AUGUST), "aug"),
                arguments(Optional.of(Month.SEPTEMBER), "sep"),
                arguments(Optional.of(Month.OCTOBER), "okt"),
                arguments(Optional.of(Month.NOVEMBER), "nov"),
                arguments(Optional.of(Month.DECEMBER), "dez")
        );
    }

    private static Stream<Arguments> parseSpecialCases() {
        return Stream.of(
                arguments(Optional.empty(), ";lkjasdf"),
                arguments(Optional.empty(), "3.2"),
                arguments(Optional.empty(), "#test#"),
                arguments(Optional.empty(), "8,"),
                arguments(Optional.empty(), "")
        );
    }

    @ParameterizedTest
    @MethodSource("parseGermanShortMonthTest")
    public void parseGermanShortMonthTest(Optional<Month> expected, String input) {
        assertEquals(expected, Month.parseGermanShortMonth(input));
    }

    private static Stream<Arguments> parseGermanShortMonthTest() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "jan"),
                arguments(Optional.of(Month.JANUARY), "januar"),
                arguments(Optional.of(Month.FEBRUARY), "feb"),
                arguments(Optional.of(Month.FEBRUARY), "februar"),
                arguments(Optional.of(Month.MARCH), "mär"),
                arguments(Optional.of(Month.MARCH), "mae"),
                arguments(Optional.of(Month.MARCH), "märz"),
                arguments(Optional.of(Month.MARCH), "maerz"),
                arguments(Optional.of(Month.APRIL), "apr"),
                arguments(Optional.of(Month.APRIL), "april"),
                arguments(Optional.of(Month.MAY), "mai"),
                arguments(Optional.of(Month.JUNE), "jun"),
                arguments(Optional.of(Month.JUNE), "juni"),
                arguments(Optional.of(Month.JULY), "jul"),
                arguments(Optional.of(Month.JULY), "juli"),
                arguments(Optional.of(Month.AUGUST), "aug"),
                arguments(Optional.of(Month.AUGUST), "august"),
                arguments(Optional.of(Month.SEPTEMBER), "sep"),
                arguments(Optional.of(Month.SEPTEMBER), "september"),
                arguments(Optional.of(Month.OCTOBER), "okt"),
                arguments(Optional.of(Month.OCTOBER), "oktober"),
                arguments(Optional.of(Month.NOVEMBER), "nov"),
                arguments(Optional.of(Month.NOVEMBER), "november"),
                arguments(Optional.of(Month.DECEMBER), "dez"),
                arguments(Optional.of(Month.DECEMBER), "dezember")
        );
    }

    @ParameterizedTest
    @MethodSource("getMonthByNumberTest")
    public void getMonthByNumberTest(Optional<Month> expected, int input) {
        assertEquals(expected, Month.getMonthByNumber(input));
    }

    private static Stream<Arguments> getMonthByNumberTest() {
        return Stream.of(
                arguments(Optional.empty(), -1),
                arguments(Optional.empty(), 0),
                arguments(Optional.of(Month.JANUARY), 1),
                arguments(Optional.of(Month.FEBRUARY), 2),
                arguments(Optional.of(Month.MARCH), 3),
                arguments(Optional.of(Month.APRIL), 4),
                arguments(Optional.of(Month.MAY), 5),
                arguments(Optional.of(Month.JUNE), 6),
                arguments(Optional.of(Month.JULY), 7),
                arguments(Optional.of(Month.AUGUST), 8),
                arguments(Optional.of(Month.SEPTEMBER), 9),
                arguments(Optional.of(Month.OCTOBER), 10),
                arguments(Optional.of(Month.NOVEMBER), 11),
                arguments(Optional.of(Month.DECEMBER), 12),
                arguments(Optional.empty(), 13),
                arguments(Optional.empty(), 14)
        );
    }

    @ParameterizedTest
    @MethodSource({"parseShortName", "getMonthByShortNameSpecialCases"})
    public void getMonthByShortNameLowercaseTest(Optional<Month> expected, String input) {
        assertEquals(expected, Month.getMonthByShortName(input));
    }

    private static Stream<Arguments> getMonthByShortNameSpecialCases() {
        return Stream.of(
                arguments(Optional.of(Month.JANUARY), "jan"),
                arguments(Optional.of(Month.JANUARY), "Jan"),
                arguments(Optional.of(Month.JANUARY), "JAN"),
                arguments(Optional.empty(), ""),
                arguments(Optional.empty(), "dez"),
                arguments(Optional.empty(), "+*ç%&/()=.,:;-${}![]^'?~¦@#°§¬|¢äüö")
        );
    }

    @ParameterizedTest
    @MethodSource("getShortNameTest")
    public void getShortNameTest(String expected, Month month) {
        assertEquals(expected, month.getShortName());
    }

    private static Stream<Arguments> getShortNameTest() {
        return Stream.of(
                arguments("jan", Month.JANUARY),
                arguments("feb", Month.FEBRUARY),
                arguments("mar", Month.MARCH),
                arguments("apr", Month.APRIL),
                arguments("may", Month.MAY),
                arguments("jun", Month.JUNE),
                arguments("jul", Month.JULY),
                arguments("aug", Month.AUGUST),
                arguments("sep", Month.SEPTEMBER),
                arguments("oct", Month.OCTOBER),
                arguments("nov", Month.NOVEMBER),
                arguments("dec", Month.DECEMBER)
        );
    }

    @ParameterizedTest
    @MethodSource("getJabRefFormatTest")
    public void getJabRefFormatTest(String expected, Month month) {
        assertEquals(expected, month.getJabRefFormat());
    }

    private static Stream<Arguments> getJabRefFormatTest() {
        return Stream.of(
                arguments("#jan#", Month.JANUARY),
                arguments("#feb#", Month.FEBRUARY),
                arguments("#mar#", Month.MARCH),
                arguments("#apr#", Month.APRIL),
                arguments("#may#", Month.MAY),
                arguments("#jun#", Month.JUNE),
                arguments("#jul#", Month.JULY),
                arguments("#aug#", Month.AUGUST),
                arguments("#sep#", Month.SEPTEMBER),
                arguments("#oct#", Month.OCTOBER),
                arguments("#nov#", Month.NOVEMBER),
                arguments("#dec#", Month.DECEMBER)
        );
    }

    @ParameterizedTest
    @MethodSource("getNumberTest")
    public void getNumberTest(int expected, Month month) {
        assertEquals(expected, month.getNumber());
    }

    private static Stream<Arguments> getNumberTest() {
        return Stream.of(
                arguments(1, Month.JANUARY),
                arguments(2, Month.FEBRUARY),
                arguments(3, Month.MARCH),
                arguments(4, Month.APRIL),
                arguments(5, Month.MAY),
                arguments(6, Month.JUNE),
                arguments(7, Month.JULY),
                arguments(8, Month.AUGUST),
                arguments(9, Month.SEPTEMBER),
                arguments(10, Month.OCTOBER),
                arguments(11, Month.NOVEMBER),
                arguments(12, Month.DECEMBER)
        );
    }

    @ParameterizedTest
    @MethodSource("getFullNameTest")
    public void getFullNameTest(String expected, Month month) {
        assertEquals(expected, month.getFullName());
    }

    private static Stream<Arguments> getFullNameTest() {
        return Stream.of(
                arguments("January", Month.JANUARY),
                arguments("February", Month.FEBRUARY),
                arguments("March", Month.MARCH),
                arguments("April", Month.APRIL),
                arguments("May", Month.MAY),
                arguments("June", Month.JUNE),
                arguments("July", Month.JULY),
                arguments("August", Month.AUGUST),
                arguments("September", Month.SEPTEMBER),
                arguments("October", Month.OCTOBER),
                arguments("November", Month.NOVEMBER),
                arguments("December", Month.DECEMBER)
        );
    }

    @ParameterizedTest
    @MethodSource("getTwoDigitNumberTest")
    public void getTwoDigitNumberTest(String expected, Month month) {
        assertEquals(expected, month.getTwoDigitNumber());
    }

    private static Stream<Arguments> getTwoDigitNumberTest() {
        return Stream.of(
                arguments("01", Month.JANUARY),
                arguments("02", Month.FEBRUARY),
                arguments("03", Month.MARCH),
                arguments("04", Month.APRIL),
                arguments("05", Month.MAY),
                arguments("06", Month.JUNE),
                arguments("07", Month.JULY),
                arguments("08", Month.AUGUST),
                arguments("09", Month.SEPTEMBER),
                arguments("10", Month.OCTOBER),
                arguments("11", Month.NOVEMBER),
                arguments("12", Month.DECEMBER)
        );
    }
}
