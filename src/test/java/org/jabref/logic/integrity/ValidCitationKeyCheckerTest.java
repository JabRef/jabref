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
    void citationKeyValidity(Optional optionalArgument, String citationKey, String errorMessage) {
        assertEquals(optionalArgument, checker.checkValue(citationKey), errorMessage);
    }

    private static Stream<Arguments> provideCitationKeys() {
        return Stream.of(Arguments.of(Optional.of(Localization.lang("empty citation key")), "", "Citation key not empty"),
                Arguments.of(Optional.empty(), "Seaver2019", "Invalid citation key"),
                Arguments.of(Optional.of(Localization.lang("Invalid citation key")), "Seaver_2019}", "Valid citation key"));
    }
}
