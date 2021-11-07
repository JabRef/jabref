package org.jabref.gui.doi;

import java.util.stream.Stream;

import org.jabref.model.entry.identifier.DOI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

  /*
  Test class for the added removeScharDOI(String inputStr) method
  The method is supposed to automatically remove the non-valid characters at the end of the input DOI
   */

public class DOITest {
    private static Stream<Arguments> testData() {
        return Stream.of(
                // Arguments.of("10.1006/jmbi.1998.2354", new DOI("10.1006/jmbi.1998.2354").getDOI()),
                Arguments.of(DOI.removeScharDOI("10.1007/978-3-319-47590-5_8?"), "10.1007/978-3-319-47590-5_8"),
                Arguments.of(DOI.removeScharDOI("10.1007/978-3-319-47590-5_8?$"), "10.1007/978-3-319-47590-5_8"),
                Arguments.of(DOI.removeScharDOI("10.1007/978-3-319-47590-5_8"), "10.1007/978-3-319-47590-5_8"),
                Arguments.of(DOI.removeScharDOI("10.1007/978-3-319-47590-?%"), "10.1007/978-3-319-47590-"),
                Arguments.of(DOI.removeScharDOI("10.1007/*?"), "10.1007/")
        );
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void testEquals(String expected, String input) {
        assertEquals(expected, input);
    }
}
