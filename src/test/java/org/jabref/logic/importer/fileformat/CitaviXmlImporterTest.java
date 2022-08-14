package org.jabref.logic.importer.fileformat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitaviXmlImporterTest {

    CitaviXmlImporter citaviXmlImporter = new CitaviXmlImporter();

    public static Stream<Arguments> cleanUpText() {
        return Stream.of(
                Arguments.of("no action", "no action"),
                Arguments.of("\\{action\\}", "{action}"),
                Arguments.of("\\}", "}"));
    }

    @ParameterizedTest
    @MethodSource
    void cleanUpText(String expected, String input) {
        assertEquals(expected, citaviXmlImporter.cleanUpText(input));
    }
}
