package org.jabref.logic.journals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbbreviationTest {

    @Test
    public void testAbbreviationsWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N. ");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getMedlineAbbreviation());
        assertEquals("L. N.", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    public void testAbbreviationsWithTrailingSpacesWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N. ", " LN ");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getMedlineAbbreviation());
        assertEquals("LN", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    public void testAbbreviationsWithSemicolons() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N.;LN;M");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getMedlineAbbreviation());
        assertEquals("L. N.;LN;M", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    public void testAbbreviationsWithSemicolonsWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N.;LN;M", "LNLNM");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getMedlineAbbreviation());
        assertEquals("LNLNM", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    public void testGetNextElement() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N.");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("Long Name", abbreviation.getNext("L N"));
    }

    @Test
    public void testGetNextElementWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N.", " LN");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("LN", abbreviation.getNext("L N"));
        assertEquals("Long Name", abbreviation.getNext("LN"));
    }

    @Test
    public void testGetNextElementWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N. ");

        assertEquals("L. N.", abbreviation.getNext(" Long Name "));
        assertEquals("L N", abbreviation.getNext(" L. N. "));
        assertEquals("Long Name", abbreviation.getNext(" L N "));
    }

    @Test
    public void testGetNextElementWithTrailingSpacesWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N. ", " LN ");

        assertEquals("L. N.", abbreviation.getNext(" Long Name "));
        assertEquals("L N", abbreviation.getNext(" L. N. "));
        assertEquals("LN", abbreviation.getNext("L N"));
        assertEquals("Long Name", abbreviation.getNext("LN"));
    }

    @Test
    public void testDefaultAndMedlineAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L N ");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getMedlineAbbreviation());
    }

    @Test
    public void testDefaultAndMedlineAbbreviationsAreSameWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L N ", " LN ");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getMedlineAbbreviation());
    }

    @Test
    public void testDefaultAndShortestUniqueAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L N ");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getShortestUniqueAbbreviation());
    }
}
