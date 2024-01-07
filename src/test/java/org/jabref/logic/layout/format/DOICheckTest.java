package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.preferences.DOIPreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DOICheckTest {

    private final DOIPreferences doiPreferences = mock(DOIPreferences.class);
    private final LayoutFormatter layoutFormatter = new DOICheck(doiPreferences);

    @ParameterizedTest
    @MethodSource("provideDOI")
    void formatDOI(String formattedDOI, String originalDOI) {
        assertEquals(formattedDOI, layoutFormatter.format(originalDOI));
    }

    @Test
    void formatDOIWithCustomBase() {
        when(doiPreferences.isUseCustom()).thenReturn(true);
        when(doiPreferences.getDefaultBaseURI()).thenReturn("http://example.org");
        assertEquals("http://example.org/10.1000/ISBN1-900512-44-0", layoutFormatter.format("10.1000/ISBN1-900512-44-0"));
    }

    private static Stream<Arguments> provideDOI() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of(null, null),
                Arguments.of("https://doi.org/10.1000/ISBN1-900512-44-0", "10.1000/ISBN1-900512-44-0"),
                Arguments.of("https://doi.org/10.1000/ISBN1-900512-44-0", "http://dx.doi.org/10.1000/ISBN1-900512-44-0"),
                Arguments.of("https://doi.org/10.1000/ISBN1-900512-44-0", "http://doi.acm.org/10.1000/ISBN1-900512-44-0"),
                Arguments.of("https://doi.org/10.1145/354401.354407", "http://doi.acm.org/10.1145/354401.354407"),
                Arguments.of("https://doi.org/10.1145/354401.354407", "10.1145/354401.354407"),
                Arguments.of("https://doi.org/10.1145/354401.354407", "/10.1145/354401.354407"),
                Arguments.of("10", "10"),
                Arguments.of("1", "1")

        );
    }
}
