package org.jabref.logic.journals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbbreviationTest {

    @Test
    public void testAbbreviationsWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N. ", " LN ");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getIsoAbbreviation());
        assertEquals("L N", abbreviation.getMedlineAbbreviation());
        assertEquals("LN", abbreviation.getShortestUnique());
    }

    @Test
    public void testAbbreviationsWithUnusedElements() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N.;LN;M", "LN");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getIsoAbbreviation());
        assertEquals("L N", abbreviation.getMedlineAbbreviation());
        assertEquals("LN", abbreviation.getShortestUnique());
    }

    @Test
    public void testGetNextElement() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N.;LN;M", "LN");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("LN", abbreviation.getNext("L N"));
        assertEquals("Long Name", abbreviation.getNext("LN"));
    }

    @Test
    public void testGetNextElementWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L. N.; LN ;M ", " LN ");

        assertEquals("L. N.", abbreviation.getNext(" Long Name "));
        assertEquals("L N", abbreviation.getNext(" L. N. "));
        assertEquals("LN", abbreviation.getNext(" L N "));
        assertEquals("Long Name", abbreviation.getNext(" LN "));
    }

    @Test
    public void testIsoAndMedlineAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation(" Long Name ", " L N ", " LN ");
        assertEquals(abbreviation.getIsoAbbreviation(), abbreviation.getMedlineAbbreviation());
    }
}
