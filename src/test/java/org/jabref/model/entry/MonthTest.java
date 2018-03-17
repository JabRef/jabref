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
}
