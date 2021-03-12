package org.jabref.model.entry;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonthTest {

    @Test
    public void parseCorrectlyByShortName() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("jan"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("feb"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("mar"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("apr"));
        assertEquals(Optional.of(Month.MAY), Month.parse("may"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("jun"));
        assertEquals(Optional.of(Month.JULY), Month.parse("jul"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("aug"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("sep"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("oct"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("nov"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("dec"));
    }

    @Test
    public void parseCorrectlyByBibtexName() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("#jan#"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("#feb#"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("#mar#"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("#apr#"));
        assertEquals(Optional.of(Month.MAY), Month.parse("#may#"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("#jun#"));
        assertEquals(Optional.of(Month.JULY), Month.parse("#jul#"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("#aug#"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("#sep#"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("#oct#"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("#nov#"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("#dec#"));
    }

    @Test
    public void parseCorrectlyByFullName() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("January"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("February"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("March"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("April"));
        assertEquals(Optional.of(Month.MAY), Month.parse("May"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("June"));
        assertEquals(Optional.of(Month.JULY), Month.parse("July"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("August"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("September"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("October"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("November"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("December"));
    }

    @Test
    public void parseCorrectlyByTwoDigitNumber() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("01"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("02"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("03"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("04"));
        assertEquals(Optional.of(Month.MAY), Month.parse("05"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("06"));
        assertEquals(Optional.of(Month.JULY), Month.parse("07"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("08"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("09"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("10"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("11"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("12"));
    }

    @Test
    public void parseCorrectlyByNumber() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("1"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("2"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("3"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("4"));
        assertEquals(Optional.of(Month.MAY), Month.parse("5"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("6"));
        assertEquals(Optional.of(Month.JULY), Month.parse("7"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("8"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("9"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("10"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("11"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("12"));
    }

    @Test
    public void parseReturnsEmptyOptionalForInvalidInput() {
        assertEquals(Optional.empty(), Month.parse(";lkjasdf"));
        assertEquals(Optional.empty(), Month.parse("3.2"));
        assertEquals(Optional.empty(), Month.parse("#test#"));
        assertEquals(Optional.empty(), Month.parse("8,"));
    }

    @Test
    public void parseReturnsEmptyOptionalForEmptyInput() {
        assertEquals(Optional.empty(), Month.parse(""));
    }

    @Test
    public void parseCorrectlyByShortNameGerman() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("Jan"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("Feb"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("Mär"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("Mae"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("Apr"));
        assertEquals(Optional.of(Month.MAY), Month.parse("Mai"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("Jun"));
        assertEquals(Optional.of(Month.JULY), Month.parse("Jul"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("Aug"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("Sep"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("Okt"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("Nov"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("Dez"));
    }

    @Test
    public void parseCorrectlyByFullNameGerman() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("Januar"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("Februar"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("März"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("Maerz"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("April"));
        assertEquals(Optional.of(Month.MAY), Month.parse("Mai"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("Juni"));
        assertEquals(Optional.of(Month.JULY), Month.parse("Juli"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("August"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("September"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("Oktober"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("November"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("Dezember"));
    }

    @Test
    public void parseCorrectlyByShortNameGermanLowercase() {
        assertEquals(Optional.of(Month.JANUARY), Month.parse("jan"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parse("feb"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("mär"));
        assertEquals(Optional.of(Month.MARCH), Month.parse("mae"));
        assertEquals(Optional.of(Month.APRIL), Month.parse("apr"));
        assertEquals(Optional.of(Month.MAY), Month.parse("mai"));
        assertEquals(Optional.of(Month.JUNE), Month.parse("jun"));
        assertEquals(Optional.of(Month.JULY), Month.parse("jul"));
        assertEquals(Optional.of(Month.AUGUST), Month.parse("aug"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("sep"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parse("okt"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parse("nov"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parse("dez"));
    }

    @Test
    public void parseGermanShortMonthTest() {
        assertEquals(Optional.of(Month.JANUARY), Month.parseGermanShortMonth("jan"));
        assertEquals(Optional.of(Month.JANUARY), Month.parseGermanShortMonth("januar"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parseGermanShortMonth("feb"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.parseGermanShortMonth("februar"));
        assertEquals(Optional.of(Month.MARCH), Month.parseGermanShortMonth("mär"));
        assertEquals(Optional.of(Month.MARCH), Month.parseGermanShortMonth("mae"));
        assertEquals(Optional.of(Month.MARCH), Month.parseGermanShortMonth("märz"));
        assertEquals(Optional.of(Month.MARCH), Month.parseGermanShortMonth("maerz"));
        assertEquals(Optional.of(Month.APRIL), Month.parseGermanShortMonth("apr"));
        assertEquals(Optional.of(Month.APRIL), Month.parseGermanShortMonth("april"));
        assertEquals(Optional.of(Month.MAY), Month.parseGermanShortMonth("mai"));
        assertEquals(Optional.of(Month.JUNE), Month.parseGermanShortMonth("jun"));
        assertEquals(Optional.of(Month.JUNE), Month.parseGermanShortMonth("juni"));
        assertEquals(Optional.of(Month.JULY), Month.parseGermanShortMonth("jul"));
        assertEquals(Optional.of(Month.JULY), Month.parseGermanShortMonth("juli"));
        assertEquals(Optional.of(Month.AUGUST), Month.parseGermanShortMonth("aug"));
        assertEquals(Optional.of(Month.AUGUST), Month.parseGermanShortMonth("august"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parseGermanShortMonth("sep"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.parseGermanShortMonth("september"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parseGermanShortMonth("okt"));
        assertEquals(Optional.of(Month.OCTOBER), Month.parseGermanShortMonth("oktober"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parseGermanShortMonth("nov"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.parseGermanShortMonth("november"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parseGermanShortMonth("dez"));
        assertEquals(Optional.of(Month.DECEMBER), Month.parseGermanShortMonth("dezember"));
    }

    @Test
    public void getMonthByValidNumberTest() {
        assertEquals(Optional.of(Month.JANUARY), Month.getMonthByNumber(1));
        assertEquals(Optional.of(Month.FEBRUARY), Month.getMonthByNumber(2));
        assertEquals(Optional.of(Month.MARCH), Month.getMonthByNumber(3));
        assertEquals(Optional.of(Month.APRIL), Month.getMonthByNumber(4));
        assertEquals(Optional.of(Month.MAY), Month.getMonthByNumber(5));
        assertEquals(Optional.of(Month.JUNE), Month.getMonthByNumber(6));
        assertEquals(Optional.of(Month.JULY), Month.getMonthByNumber(7));
        assertEquals(Optional.of(Month.AUGUST), Month.getMonthByNumber(8));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.getMonthByNumber(9));
        assertEquals(Optional.of(Month.OCTOBER), Month.getMonthByNumber(10));
        assertEquals(Optional.of(Month.NOVEMBER), Month.getMonthByNumber(11));
        assertEquals(Optional.of(Month.DECEMBER), Month.getMonthByNumber(12));
    }

    @Test
    public void getMonthByInvalidNumberTest() {
        assertEquals(Optional.empty(), Month.getMonthByNumber(-1));
        assertEquals(Optional.empty(), Month.getMonthByNumber(0));
        assertEquals(Optional.empty(), Month.getMonthByNumber(13));
    }

    @Test
    public void getMonthByShortNameCaseSensitivityTest() {
        assertEquals(Optional.of(Month.JANUARY), Month.getMonthByShortName("jan"));
        assertEquals(Optional.of(Month.JANUARY), Month.getMonthByShortName("JAN"));
        assertEquals(Optional.of(Month.JANUARY), Month.getMonthByShortName("Jan"));
    }

    @Test
    public void getMonthByShortNameLowercaseTest() {
        assertEquals(Optional.of(Month.JANUARY), Month.getMonthByShortName("jan"));
        assertEquals(Optional.of(Month.FEBRUARY), Month.getMonthByShortName("feb"));
        assertEquals(Optional.of(Month.MARCH), Month.getMonthByShortName("mar"));
        assertEquals(Optional.of(Month.APRIL), Month.getMonthByShortName("apr"));
        assertEquals(Optional.of(Month.MAY), Month.getMonthByShortName("may"));
        assertEquals(Optional.of(Month.JUNE), Month.getMonthByShortName("jun"));
        assertEquals(Optional.of(Month.JULY), Month.getMonthByShortName("jul"));
        assertEquals(Optional.of(Month.AUGUST), Month.getMonthByShortName("aug"));
        assertEquals(Optional.of(Month.SEPTEMBER), Month.getMonthByShortName("sep"));
        assertEquals(Optional.of(Month.OCTOBER), Month.getMonthByShortName("oct"));
        assertEquals(Optional.of(Month.NOVEMBER), Month.getMonthByShortName("nov"));
        assertEquals(Optional.of(Month.DECEMBER), Month.getMonthByShortName("dec"));
    }

    @Test
    public void getMonthByShortNameSpecialCharactersTest() {
        assertEquals(Optional.empty(), Month.getMonthByShortName(""));
        assertEquals(Optional.empty(), Month.getMonthByShortName("dez"));
        assertEquals(Optional.empty(), Month.getMonthByShortName("+*ç%&/()=.,:;-${}![]^'?~¦@#°§¬|¢äüö"));
    }

    @Test
    public void getShortNameTest() {
        assertEquals("jan", Month.JANUARY.getShortName());
        assertEquals("feb", Month.FEBRUARY.getShortName());
        assertEquals("mar", Month.MARCH.getShortName());
        assertEquals("apr", Month.APRIL.getShortName());
        assertEquals("may", Month.MAY.getShortName());
        assertEquals("jun", Month.JUNE.getShortName());
        assertEquals("jul", Month.JULY.getShortName());
        assertEquals("aug", Month.AUGUST.getShortName());
        assertEquals("sep", Month.SEPTEMBER.getShortName());
        assertEquals("oct", Month.OCTOBER.getShortName());
        assertEquals("nov", Month.NOVEMBER.getShortName());
        assertEquals("dec", Month.DECEMBER.getShortName());

    }

    @Test
    public void getJabRefFormatTest() {
        assertEquals("#jan#", Month.JANUARY.getJabRefFormat());
        assertEquals("#feb#", Month.FEBRUARY.getJabRefFormat());
        assertEquals("#mar#", Month.MARCH.getJabRefFormat());
        assertEquals("#apr#", Month.APRIL.getJabRefFormat());
        assertEquals("#may#", Month.MAY.getJabRefFormat());
        assertEquals("#jun#", Month.JUNE.getJabRefFormat());
        assertEquals("#jul#", Month.JULY.getJabRefFormat());
        assertEquals("#aug#", Month.AUGUST.getJabRefFormat());
        assertEquals("#sep#", Month.SEPTEMBER.getJabRefFormat());
        assertEquals("#oct#", Month.OCTOBER.getJabRefFormat());
        assertEquals("#nov#", Month.NOVEMBER.getJabRefFormat());
        assertEquals("#dec#", Month.DECEMBER.getJabRefFormat());
    }

    @Test
    public void getNumberTest() {
        assertEquals(1, Month.JANUARY.getNumber());
        assertEquals(2, Month.FEBRUARY.getNumber());
        assertEquals(3, Month.MARCH.getNumber());
        assertEquals(4, Month.APRIL.getNumber());
        assertEquals(5, Month.MAY.getNumber());
        assertEquals(6, Month.JUNE.getNumber());
        assertEquals(7, Month.JULY.getNumber());
        assertEquals(8, Month.AUGUST.getNumber());
        assertEquals(9, Month.SEPTEMBER.getNumber());
        assertEquals(10, Month.OCTOBER.getNumber());
        assertEquals(11, Month.NOVEMBER.getNumber());
        assertEquals(12, Month.DECEMBER.getNumber());
    }

    @Test
    public void getFullNameTest() {
        assertEquals("January", Month.JANUARY.getFullName());
        assertEquals("February", Month.FEBRUARY.getFullName());
        assertEquals("March", Month.MARCH.getFullName());
        assertEquals("April", Month.APRIL.getFullName());
        assertEquals("May", Month.MAY.getFullName());
        assertEquals("June", Month.JUNE.getFullName());
        assertEquals("July", Month.JULY.getFullName());
        assertEquals("August", Month.AUGUST.getFullName());
        assertEquals("September", Month.SEPTEMBER.getFullName());
        assertEquals("October", Month.OCTOBER.getFullName());
        assertEquals("November", Month.NOVEMBER.getFullName());
        assertEquals("December", Month.DECEMBER.getFullName());
    }

    @Test
    public void getTwoDigitNumber() {
        assertEquals("01", Month.JANUARY.getTwoDigitNumber());
        assertEquals("02", Month.FEBRUARY.getTwoDigitNumber());
        assertEquals("03", Month.MARCH.getTwoDigitNumber());
        assertEquals("04", Month.APRIL.getTwoDigitNumber());
        assertEquals("05", Month.MAY.getTwoDigitNumber());
        assertEquals("06", Month.JUNE.getTwoDigitNumber());
        assertEquals("07", Month.JULY.getTwoDigitNumber());
        assertEquals("08", Month.AUGUST.getTwoDigitNumber());
        assertEquals("09", Month.SEPTEMBER.getTwoDigitNumber());
        assertEquals("10", Month.OCTOBER.getTwoDigitNumber());
        assertEquals("11", Month.NOVEMBER.getTwoDigitNumber());
        assertEquals("12", Month.DECEMBER.getTwoDigitNumber());
    }
}
