package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class FileLinkTest {

    private FileLinkPreferences prefs;
    private ParamLayoutFormatter fileLinkLayoutFormatter;

    @BeforeEach
    public void setUp() throws Exception {
        prefs = mock(FileLinkPreferences.class);
        fileLinkLayoutFormatter = new FileLink(prefs);
    }

    @ParameterizedTest
    @MethodSource("provideFileLinks")
    void formatFileLinks(String formattedFileLink, String originalFileLink, String desiredDocType) {
        if (!desiredDocType.isEmpty()) {
            fileLinkLayoutFormatter.setArgument(desiredDocType);
        }
        assertEquals(formattedFileLink, fileLinkLayoutFormatter.format(originalFileLink));
    }

    private static Stream<Arguments> provideFileLinks() {
        return Stream.of(
                Arguments.of("", "", ""),
                Arguments.of("", null, ""),
                Arguments.of("test.pdf", "test.pdf", ""),
                Arguments.of("test.pdf", "paper:test.pdf:PDF", ""),
                Arguments.of("test.pdf", "paper:test.pdf:PDF;presentation:pres.ppt:PPT", ""),
                Arguments.of("pres.ppt", "paper:test.pdf:PDF;presentation:pres.ppt:PPT", "ppt"),
                Arguments.of("", "paper:test.pdf:PDF;presentation:pres.ppt:PPT", "doc")
        );
    }
}
