package org.jabref.logic.journals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AbbreviationTest {

    @Test
    void testAbbreviationsWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getDotlessAbbreviation());
        assertEquals("L. N.", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testAbbreviationsWithTrailingSpacesWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.", "LN");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getDotlessAbbreviation());
        assertEquals("LN", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testAbbreviationsWithSemicolons() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.;LN;M");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getDotlessAbbreviation());
        assertEquals("L. N.;LN;M", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testAbbreviationsWithSemicolonsWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.;LN;M", "LNLNM");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getDotlessAbbreviation());
        assertEquals("LNLNM", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testGetNextElement() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("Long Name", abbreviation.getNext("L N"));
    }

    @Test
    void testGetNextElementWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.", "LN");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("LN", abbreviation.getNext("L N"));
        assertEquals("Long Name", abbreviation.getNext("LN"));
    }

    @Test
    void testGetNextElementWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("Long Name", abbreviation.getNext("L N"));
    }

    @Test
    void testGetNextElementWithTrailingSpacesWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.", "LN");

        assertEquals("L. N.", abbreviation.getNext("Long Name"));
        assertEquals("L N", abbreviation.getNext("L. N."));
        assertEquals("LN", abbreviation.getNext("L N"));
        assertEquals("Long Name", abbreviation.getNext("LN"));
    }

    @Test
    void testDefaultAndMedlineAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getDotlessAbbreviation());
    }

    @Test
    void testDefaultAndMedlineAbbreviationsAreSameWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N", "LN");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getDotlessAbbreviation());
    }

    @Test
    void testDefaultAndShortestUniqueAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testEquals() {
      Abbreviation abbreviation = new Abbreviation("Long Name", "L N", "LN");
      Abbreviation otherAbbreviation = new Abbreviation("Long Name", "L N", "LN");
      assertEquals(abbreviation, otherAbbreviation);
      assertNotEquals(abbreviation, "String");
    }

    @Test
    void equalAbbrevationsWithFourComponentsAreAlsoCompareZero() {
        Abbreviation abbreviation1 = new Abbreviation("Long Name", "L. N.", "LN");
        Abbreviation abbreviation2 = new Abbreviation("Long Name", "L. N.", "LN");
        assertEquals(0, abbreviation1.compareTo(abbreviation2));
    }
}
