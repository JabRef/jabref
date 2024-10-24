package org.jabref.model.entry;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LangidTest {

    @ParameterizedTest
    @MethodSource({"parseLangidTest", "getByLangidTest", "invalidLangidTest"})
    void parseCorrectly(Optional<String> expectedLangid, String input) {
        Optional<Langid> parsedLangid = Langid.parse(input);

        if (expectedLangid.isPresent()) {
            // If the expected langid is present, check that the parsed Langid is present and matches the expected value
            assertTrue(parsedLangid.isPresent(), "Langid should be present for input: " + input);
            assertEquals(expectedLangid.get(), parsedLangid.get().getLangid(), "Langid value mismatch for input: " + input);
        } else {
            // If the expected langid is empty, check that the parsed Langid is empty as well
            assertFalse(parsedLangid.isPresent(), "Langid should not be present for input: " + input);
        }
    }

    private static Stream<Arguments> parseLangidTest() {
        return Stream.of(
                arguments(Optional.of("basque"), "basque"),
                arguments(Optional.of("bulgarian"), "bulgarian"),
                arguments(Optional.of("american"), "american"),  // Both AMERICAN and USENGLISH have the same langid
                arguments(Optional.of("british"), "british"),    // Both BRITISH and UKENGLISH have the same langid
                arguments(Optional.of("hungarian"), "hungarian") // Both MAGYAR and HUNGARIAN have the same langid
        );
    }

    private static Stream<Arguments> getByLangidTest() {
        return Stream.of(
                arguments(Optional.of("basque"), "Basque"),
                arguments(Optional.of("bulgarian"), "Bulgarian"),
                arguments(Optional.of("american"), "american"),  // Ensures "american" maps to either AMERICAN or USENGLISH
                arguments(Optional.of("british"), "british"),    // Ensures "british" maps to either BRITISH or UKENGLISH
                arguments(Optional.of("hungarian"), "hungarian") // Ensures "hungarian" maps to either MAGYAR or HUNGARIAN
        );
    }

    private static Stream<Arguments> invalidLangidTest() {
        return Stream.of(
                arguments(Optional.empty(), "invalidlangid"),  // Invalid langid should return empty
                arguments(Optional.empty(), "12345"),          // Non-langid input should return empty
                arguments(Optional.empty(), "")                // Empty string should return empty
        );
    }

    @Test
    public void testMultipleEnumsMappingToSameLangid() {
        assertEquals(Langid.AMERICAN.getLangid(), Langid.USENGLISH.getLangid());
        assertEquals(Langid.BRITISH.getLangid(), Langid.UKENGLISH.getLangid());
        assertEquals(Langid.MAGYAR.getLangid(), Langid.HUNGARIAN.getLangid());
        assertEquals(Langid.PORTUGUESE.getLangid(), Langid.PORTUGES.getLangid());
        assertEquals(Langid.SLOVENE.getLangid(), Langid.SLOVENIAN.getLangid());
    }

    @ParameterizedTest
    @MethodSource("getLangidByStringTest")
    void getLangidByStringTest(Optional<String> expectedLangid, String input) {
        Optional<Langid> parsedLangid = Langid.getByLangid(input);

        if (expectedLangid.isPresent()) {
            assertTrue(parsedLangid.isPresent(), "Langid should be present for input: " + input);
            assertEquals(expectedLangid.get(), parsedLangid.get().getLangid(), "Langid value mismatch for input: " + input);
        } else {
            assertFalse(parsedLangid.isPresent(), "Langid should not be present for input: " + input);
        }
    }

    private static Stream<Arguments> getLangidByStringTest() {
        return Stream.of(
                arguments(Optional.of("basque"), "basque"),
                arguments(Optional.of("bulgarian"), "bulgarian"),
                arguments(Optional.empty(), "unknown")
        );
    }

    @ParameterizedTest
    @MethodSource("getLangidNameTest")
    void getLangidNameTest(String expected, Langid langid) {
        assertEquals(expected, langid.getLangid());
    }

    private static Stream<Arguments> getLangidNameTest() {
        return Stream.of(
                arguments("basque", Langid.BASQUE),
                arguments("bulgarian", Langid.BULGARIAN)
        );
    }
}
