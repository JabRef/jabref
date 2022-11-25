package org.jabref.logic.integrity;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ValidCitationKeyCheckerTest {

    private final ValidCitationKeyChecker checker = new ValidCitationKeyChecker();

    @ParameterizedTest
    @MethodSource("provideCitationKeys")
    void citationKeyValidity(Optional optionalArgument, String citationKey) {
        assertEquals(optionalArgument, checker.checkValue(citationKey));
    }

    private static Stream<Arguments> provideCitationKeys() {
        return Stream.of(
                Arguments.of(Optional.of(Localization.lang("empty citation key")), ""),
                Arguments.of(Optional.empty(), "Seaver2019"),
                Arguments.of(Optional.of(Localization.lang("Invalid citation key")), "Seaver_2019}")
        );
    }
}
