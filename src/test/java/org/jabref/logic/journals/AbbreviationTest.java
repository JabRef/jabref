package org.jabref.logic.journals;

import java.util.stream.Stream;

import org.jabref.gui.preferences.journals.AbbreviationViewModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbbreviationTest {

    @Test
    void testAbbreviationsWithTrailingSpaces() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getMedlineAbbreviation());
        assertEquals("L. N.", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testAbbreviationsWithTrailingSpacesWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.", "LN");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.", abbreviation.getAbbreviation());
        assertEquals("L N", abbreviation.getMedlineAbbreviation());
        assertEquals("LN", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testAbbreviationsWithSemicolons() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.;LN;M");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getMedlineAbbreviation());
        assertEquals("L. N.;LN;M", abbreviation.getShortestUniqueAbbreviation());
    }

    @Test
    void testAbbreviationsWithSemicolonsWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L. N.;LN;M", "LNLNM");

        assertEquals("Long Name", abbreviation.getName());
        assertEquals("L. N.;LN;M", abbreviation.getAbbreviation());
        assertEquals("L N ;LN;M", abbreviation.getMedlineAbbreviation());
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
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getMedlineAbbreviation());
    }

    @Test
    void testDefaultAndMedlineAbbreviationsAreSameWithShortestUniqueAbbreviation() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N", "LN");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getMedlineAbbreviation());
    }

    @Test
    void testDefaultAndShortestUniqueAbbreviationsAreSame() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N");
        assertEquals(abbreviation.getAbbreviation(), abbreviation.getShortestUniqueAbbreviation());
    }

    @ParameterizedTest
    @MethodSource("provideTestStringIsFoundInAllAbbreviationFields")
    void testStringIsFoundInAllAbbreviationFields(String searchTerm, AbbreviationViewModel abbreviation, boolean expected) {
        assertEquals(expected, abbreviation.containsCaseIndependent(searchTerm));
    }

    private static Stream<Arguments> provideTestStringIsFoundInAllAbbreviationFields() {
        return Stream.of(
                Arguments.of("name", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique")), true),
                Arguments.of("bBr", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique")), true),
                Arguments.of("Uniq", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique")), true),
                Arguments.of("Something else", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique")), false),
                Arguments.of("", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique")), true),
                Arguments.of("", new AbbreviationViewModel(new Abbreviation("", "", "")), true),
                Arguments.of("Something", new AbbreviationViewModel(new Abbreviation("", "", "")), false)
        );
    }
}
