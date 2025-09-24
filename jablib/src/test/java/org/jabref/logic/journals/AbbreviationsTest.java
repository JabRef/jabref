package org.jabref.logic.journals;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbbreviationsTest {

    private JournalAbbreviationRepository repository;

    @BeforeEach
    void setUp() {
        repository = JournalAbbreviationLoader.loadBuiltInRepository();
    }

    @ParameterizedTest(name = "getNextAbbreviation(\"{1}\") should return \"{0}\"")
    @MethodSource("provideAbbreviationTestCases")
    void getNextAbbreviation(String expected, String input) {
        assertEquals(expected, repository.getNextAbbreviation(input).get());
    }

    private static Stream<Arguments> provideAbbreviationTestCases() {
        return Stream.of(
                // Test abbreviating full journal title
                Arguments.of("2D Mater.", "2D Materials"),

                // Test converting abbreviation to dotless abbreviation
                Arguments.of("2D Mater", "2D Mater.")
        );
    }
}
