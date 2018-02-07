package org.jabref.model.entry;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class MonthTest {

    @Test
    public void parseCorrectlyByShortName() {
        Assert.assertEquals(Optional.of(Month.JANUARY), Month.parse("jan"));
        Assert.assertEquals(Optional.of(Month.FEBRUARY), Month.parse("feb"));
        Assert.assertEquals(Optional.of(Month.MARCH), Month.parse("mar"));
        Assert.assertEquals(Optional.of(Month.APRIL), Month.parse("apr"));
        Assert.assertEquals(Optional.of(Month.MAY), Month.parse("may"));
        Assert.assertEquals(Optional.of(Month.JUNE), Month.parse("jun"));
        Assert.assertEquals(Optional.of(Month.JULY), Month.parse("jul"));
        Assert.assertEquals(Optional.of(Month.AUGUST), Month.parse("aug"));
        Assert.assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("sep"));
        Assert.assertEquals(Optional.of(Month.OCTOBER), Month.parse("oct"));
        Assert.assertEquals(Optional.of(Month.NOVEMBER), Month.parse("nov"));
        Assert.assertEquals(Optional.of(Month.DECEMBER), Month.parse("dec"));
    }

    @Test
    public void parseCorrectlyByBibtexName() {
        Assert.assertEquals(Optional.of(Month.JANUARY), Month.parse("#jan#"));
        Assert.assertEquals(Optional.of(Month.FEBRUARY), Month.parse("#feb#"));
        Assert.assertEquals(Optional.of(Month.MARCH), Month.parse("#mar#"));
        Assert.assertEquals(Optional.of(Month.APRIL), Month.parse("#apr#"));
        Assert.assertEquals(Optional.of(Month.MAY), Month.parse("#may#"));
        Assert.assertEquals(Optional.of(Month.JUNE), Month.parse("#jun#"));
        Assert.assertEquals(Optional.of(Month.JULY), Month.parse("#jul#"));
        Assert.assertEquals(Optional.of(Month.AUGUST), Month.parse("#aug#"));
        Assert.assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("#sep#"));
        Assert.assertEquals(Optional.of(Month.OCTOBER), Month.parse("#oct#"));
        Assert.assertEquals(Optional.of(Month.NOVEMBER), Month.parse("#nov#"));
        Assert.assertEquals(Optional.of(Month.DECEMBER), Month.parse("#dec#"));
    }

    @Test
    public void parseCorrectlyByFullName() {
        Assert.assertEquals(Optional.of(Month.JANUARY), Month.parse("January"));
        Assert.assertEquals(Optional.of(Month.FEBRUARY), Month.parse("February"));
        Assert.assertEquals(Optional.of(Month.MARCH), Month.parse("March"));
        Assert.assertEquals(Optional.of(Month.APRIL), Month.parse("April"));
        Assert.assertEquals(Optional.of(Month.MAY), Month.parse("May"));
        Assert.assertEquals(Optional.of(Month.JUNE), Month.parse("June"));
        Assert.assertEquals(Optional.of(Month.JULY), Month.parse("July"));
        Assert.assertEquals(Optional.of(Month.AUGUST), Month.parse("August"));
        Assert.assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("September"));
        Assert.assertEquals(Optional.of(Month.OCTOBER), Month.parse("October"));
        Assert.assertEquals(Optional.of(Month.NOVEMBER), Month.parse("November"));
        Assert.assertEquals(Optional.of(Month.DECEMBER), Month.parse("December"));
    }

    @Test
    public void parseCorrectlyByTwoDigitNumber() {
        Assert.assertEquals(Optional.of(Month.JANUARY), Month.parse("01"));
        Assert.assertEquals(Optional.of(Month.FEBRUARY), Month.parse("02"));
        Assert.assertEquals(Optional.of(Month.MARCH), Month.parse("03"));
        Assert.assertEquals(Optional.of(Month.APRIL), Month.parse("04"));
        Assert.assertEquals(Optional.of(Month.MAY), Month.parse("05"));
        Assert.assertEquals(Optional.of(Month.JUNE), Month.parse("06"));
        Assert.assertEquals(Optional.of(Month.JULY), Month.parse("07"));
        Assert.assertEquals(Optional.of(Month.AUGUST), Month.parse("08"));
        Assert.assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("09"));
        Assert.assertEquals(Optional.of(Month.OCTOBER), Month.parse("10"));
        Assert.assertEquals(Optional.of(Month.NOVEMBER), Month.parse("11"));
        Assert.assertEquals(Optional.of(Month.DECEMBER), Month.parse("12"));
    }

    @Test
    public void parseCorrectlyByNumber() {
        Assert.assertEquals(Optional.of(Month.JANUARY), Month.parse("1"));
        Assert.assertEquals(Optional.of(Month.FEBRUARY), Month.parse("2"));
        Assert.assertEquals(Optional.of(Month.MARCH), Month.parse("3"));
        Assert.assertEquals(Optional.of(Month.APRIL), Month.parse("4"));
        Assert.assertEquals(Optional.of(Month.MAY), Month.parse("5"));
        Assert.assertEquals(Optional.of(Month.JUNE), Month.parse("6"));
        Assert.assertEquals(Optional.of(Month.JULY), Month.parse("7"));
        Assert.assertEquals(Optional.of(Month.AUGUST), Month.parse("8"));
        Assert.assertEquals(Optional.of(Month.SEPTEMBER), Month.parse("9"));
        Assert.assertEquals(Optional.of(Month.OCTOBER), Month.parse("10"));
        Assert.assertEquals(Optional.of(Month.NOVEMBER), Month.parse("11"));
        Assert.assertEquals(Optional.of(Month.DECEMBER), Month.parse("12"));
    }

    @Test
    public void parseReturnsEmptyOptionalForInvalidInput() {
        Assert.assertEquals(Optional.empty(), Month.parse(";lkjasdf"));
        Assert.assertEquals(Optional.empty(), Month.parse("3.2"));
        Assert.assertEquals(Optional.empty(), Month.parse("#test#"));
        Assert.assertEquals(Optional.empty(), Month.parse("8,"));
    }

    @Test
    public void parseReturnsEmptyOptionalForEmptyInput() {
        Assert.assertEquals(Optional.empty(), Month.parse(""));
    }
}
