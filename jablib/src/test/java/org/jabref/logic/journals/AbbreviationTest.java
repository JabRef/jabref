package org.jabref.logic.journals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AbbreviationTest {

    @Test
    void abbreviationsWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getDotlessAbbreviation());
        assertEquals("L. N.", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void abbreviationsWithTrailingSpacesWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.", "LN");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getDotlessAbbreviation());
        assertEquals("LN", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void abbreviationsWithSemicolons() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.;LN;M");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getDotlessAbbreviation());
        assertEquals("L. N.;LN;M", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void abbreviationsWithSemicolonsWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.;LN;M", "LNLNM");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getDotlessAbbreviation());
        assertEquals("LNLNM", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void getNextElement() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("Long Name", abbreviation.getNext("L N"));
    }

    @Test
    void getNextElementWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.", "LN");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("LN", abbreviation.getNext("L N"));
        assertEquals("Long Name", abbreviation.getNext("LN"));
    }

    @Test
    void getNextElementWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("Long Name", abbreviation.getNext("L N"));
    }

    @Test
    void getNextElementWithTrailingSpacesWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.", "LN");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("LN", abbreviation.getNext("L N"));
        assertEquals("Long Name", abbreviation.getNext("LN"));
    }

    @Test
    void defaultAndMedlineAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getDotlessAbbreviation());
    }

    @Test
    void defaultAndMedlineAbbreviationsAreSameWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N", "LN");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getDotlessAbbreviation());
    }

    @Test
    void defaultAndShortestUniqueAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void equals() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N", "LN");
        Abbreviation otherAbbreviation = new Abbreviation("Long Name", "L N", "LN");
        assertEquals(abbreviation, otherAbbreviation);
        assertNotEquals("String", abbreviation);
    }

    @Test
    void equalAbbrevationsWithFourComponentsAreAlsoCompareZero() {
        Abbreviation abbreviation1 = new Abbreviation("Long Name", "L. N.", "LN");
        Abbreviation abbreviation2 = new Abbreviation("Long Name", "L. N.", "LN");
        assertEquals(0, abbreviation1.compareTo(abbreviation2));
    }
}
