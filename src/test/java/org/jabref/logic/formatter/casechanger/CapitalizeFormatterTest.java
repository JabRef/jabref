package org.jabref.logic.formatter.casechanger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class CapitalizeFormatterTest {

    private CapitalizeFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new CapitalizeFormatter();
    }

    @Test
    public void formatExample() {
        assertEquals("I Have {a} Dream", formatter.format(formatter.getExampleInput()));
    }

    @ParameterizedTest(name = "input={0}, formattedStr={1}")
    @CsvSource(value = {
            "{}, {}", // {}
            "{upper, {upper", // unmatched braces
            "upper, Upper", // single word lower case
            "Upper, Upper", // single word correct
            "UPPER, Upper", // single word upper case
            "upper each first, Upper Each First", // multiple words lower case
            "Upper Each First, Upper Each First", // multiple words correct
            "UPPER EACH FIRST, Upper Each First", // multiple words upper case
            "upper each First, Upper Each First", // multiple words in lower and upper case
            "{u}pp{e}r, {u}pp{e}r", // single word lower case with {}
            "{U}pp{e}r, {U}pp{e}r", // single word correct with {}
            "{U}PP{E}R, {U}pp{E}r", // single word upper case with {}
            "upper each {NOT} first, Upper Each {NOT} First", // multiple words lower case with {}
            "Upper {E}ach {NOT} First, Upper {E}ach {NOT} First", // multiple words correct with {}
            "UPPER {E}ACH {NOT} FIRST, Upper {E}ach {NOT} First", // multiple words upper case with {}
            "upper each first {NOT} {this}, Upper Each First {NOT} {this}", // multiple words in lower and upper case with {}
            "upper each first {N}OT {t}his, Upper Each First {N}ot {t}his", // multiple words in lower and upper case with {} part 2
            "{with-hyphen, {with-hyphen", // unmatched braces with hyphen
            "with-hyphen, With-Hyphen", // single word with hyphen
            "With-hyphen, With-Hyphen", // single word with hyphen
            "With-Hyphen, With-Hyphen", // single word with hyphen correct
            "WITH-HYPHEN, With-Hyphen", // single word upper case with hyphen
            "multiple-words with-hyphen right-now people, Multiple-Words With-Hyphen Right-Now People", // multiple words lower case with hyphen
            "Multiple-words With-hyphen Right-now people, Multiple-Words With-Hyphen Right-Now People", // multiple words in upper and lower case with hyphen
            "Multiple-Words With-Hyphen Right-Now People, Multiple-Words With-Hyphen Right-Now People", // multiple words with hyphen correct
            "MULTIPLE-WORDS WITH-HYPHEN RIGHT-NOW PEOPLE, Multiple-Words With-Hyphen Right-Now People", // multiple words upper case with hyphen
            "{w}ith-hyphen, {w}ith-Hyphen", // single word lower case with hyphen and {}
            "{W}ith-hyphen, {W}ith-Hyphen", // single word with hyphen and {}
            "{W}ith-Hyphen, {W}ith-Hyphen", // single word correct with hyphen and {}
            "{W}ITH-HYPH{E}N, {W}ith-Hyph{E}n", // single word upper case with hyphen and {}
            "multiple-{words} with-{h}yphen right-now {people}, Multiple-{words} With-{h}yphen Right-Now {people}", // multiple words lower case with hyphen and {}
            "Multiple-{words} With-{h}yphen Right-Now {people}, Multiple-{words} With-{h}yphen Right-Now {people}", // multiple words correct with hyphen and {}
            "MULTIPLE-{words} WITH-{h}yphen RIGHT-NOW {people}, Multiple-{words} With-{h}yphen Right-Now {people}", // multiple words upper case with hyphen and {}
            "with\u002Dother dashes, With\u002DOther Dashes", // words with hyphen-minus
            "with\u058Aother dashes, With\u058AOther Dashes", // words with armenian hyphen
            "with\u05BEother dashes, With\u05BEOther Dashes", // words with hebrew punctuation maqaf
            "with\u1400other dashes, With\u1400Other Dashes", // words with canadian syllabics hyphen
            "with\u1806other dashes, With\u1806Other Dashes", // words with mongolian todo soft hyphen
            "with\u2010other dashes, With\u2010Other Dashes", // words with hyphen
            "with\u2011other dashes, With\u2011Other Dashes", // words with non-breaking hyphen
            "with\u2012other dashes, With\u2012Other Dashes", // words with figure dash
            "with\u2013other dashes, With\u2013Other Dashes", // words with en dash
            "with\u2014other dashes, With\u2014Other Dashes", // words with em dash
            "with\u2015other dashes, With\u2015Other Dashes", // words with horizontal bar
            "with\u2E17other dashes, With\u2E17Other Dashes", // words with double oblique hyphen
            "with\u2E1Aother dashes, With\u2E1AOther Dashes", // words with hyphen with diaeresis
            "with\u2E3Aother dashes, With\u2E3AOther Dashes", // words with two-em dash
            "with\u2E3Bother dashes, With\u2E3BOther Dashes", // words with three-em dash
            "with\u2E40other dashes, With\u2E40Other Dashes", // words with double hyphen
            "with\u301Cother dashes, With\u301COther Dashes", // words with wave dash
            "with\u3030other dashes, With\u3030Other Dashes", // words with wavy dash
            "with\u30A0other dashes, With\u30A0Other Dashes", // words with katakana-hiraga double hyphen
            "with\uFE31other dashes, With\uFE31Other Dashes", // words with vertical em dash
            "with\uFE32other dashes, With\uFE32Other Dashes", // words with vertical en dash
            "with\uFE58other dashes, With\uFE58Other Dashes", // words with small em dash
            "with\uFE63other dashes, With\uFE63Other Dashes", // words with small hyphen-minus
            "with\uFF0Dother dashes, With\uFF0DOther Dashes", // words with fullwidth hyphen-minus
            "remove\u200Bzero\u200Bwidth\u200Bspaces\u200B, Removezerowidthspaces", // words with unwelcome zero width spaces
            "remove\u200Czero\u200Cwidth\u200Cspaces\u200C, Removezerowidthspaces", // words with unwelcome zero width spaces
            "remove\u200Dzero\u200Dwidth\u200Dspaces\u200D, Removezerowidthspaces", // words with unwelcome zero width spaces
            "remove\uFEFFzero\uFEFFwidth\uFEFFspaces\uFEFF, Removezerowidthspaces", // words with unwelcome zero width spaces
    })
    public void testInputs(String input, String expectedResult) {
        String formattedStr = formatter.format(input);
        assertEquals(expectedResult, formattedStr);
    }
}
