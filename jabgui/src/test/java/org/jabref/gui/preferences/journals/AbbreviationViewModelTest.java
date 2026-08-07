package org.jabref.gui.preferences.journals;

import java.util.stream.Stream;

import org.jabref.logic.journals.Abbreviation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbbreviationViewModelTest {

    @ParameterizedTest
    @MethodSource("provideContainsCaseIndependentContains")
    void containsCaseIndependentContains(String searchTerm, AbbreviationViewModel abbreviation) {
        assertTrue(abbreviation.containsCaseIndependent(searchTerm));
    }

    private static Stream<Arguments> provideContainsCaseIndependentContains() {
        return Stream.of(
                Arguments.of("name", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique"))),
                Arguments.of("bBr", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique"))),
                Arguments.of("Uniq", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique"))),
                Arguments.of("", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique"))),
                Arguments.of("", new AbbreviationViewModel(new Abbreviation("", "", "")))
        );
    }

    @ParameterizedTest
    @MethodSource("provideContainsCaseIndependentDoesNotContain")
    void containsCaseIndependentDoesNotContain(String searchTerm, AbbreviationViewModel abbreviation) {
        assertFalse(abbreviation.containsCaseIndependent(searchTerm));
    }

    private static Stream<Arguments> provideContainsCaseIndependentDoesNotContain() {
        return Stream.of(
                Arguments.of("Something else", new AbbreviationViewModel(new Abbreviation("Long Name", "abbr", "unique"))),
                Arguments.of("Something", new AbbreviationViewModel(new Abbreviation("", "", "")))
        );
    }
}
