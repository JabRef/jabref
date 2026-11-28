package org.jabref.logic.l10n;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ResourceLock("Localization.lang")
class LocalizationKeyParamsTest {

    @ParameterizedTest
    @MethodSource("provideTestData")
    void replacePlaceholders(String expected, LocalizationKeyParams input) {
        assertEquals(expected, input.replacePlaceholders());
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of("biblatex mode", new LocalizationKeyParams("biblatex mode")),
                Arguments.of("biblatex mode", new LocalizationKeyParams("%0 mode", "biblatex")),
                Arguments.of("C:\\bla mode", new LocalizationKeyParams("%0 mode", "C:\\bla")),
                Arguments.of("What \n : %e %c a b", new LocalizationKeyParams("What \n : %e %c %0 %1", "a", "b")),
                Arguments.of("What \n : %e %c_a b", new LocalizationKeyParams("What \n : %e %c_%0 %1", "a", "b")),
                Arguments.of("$1 mode", new LocalizationKeyParams("%0 mode", "$1")),
                Arguments.of("File 'b' is a duplicate of 'a'. Keeping 'a'", new LocalizationKeyParams("File '%1' is a duplicate of '%0'. Keeping '%0'", "a", "b")),
                Arguments.of("a and %5", new LocalizationKeyParams("%0 and %5", "a"))
        );
    }

    /// A file name or a URL may well hold what looks like a placeholder, `%20` above all.
    @ParameterizedTest
    @MethodSource
    void aParameterIsNotTakenForAPlaceholder(String expected, LocalizationKeyParams input) {
        assertEquals(expected, input.replacePlaceholders());
    }

    private static Stream<Arguments> aParameterIsNotTakenForAPlaceholder() {
        return Stream.of(
                Arguments.of("\"x%1Fy \", should be \"x%1Fy\"", new LocalizationKeyParams("\"%0\", should be \"%1\"", "x%1Fy ", "x%1Fy")),
                Arguments.of("Pasted 3 entry(s) to my%20library.bib. 1 were skipped", new LocalizationKeyParams("Pasted %0 entry(s) to %1. %2 were skipped", "3", "my%20library.bib", "1")),
                Arguments.of("File 'b' is a duplicate of 'report%1.pdf'. Keeping 'report%1.pdf'", new LocalizationKeyParams("File '%1' is a duplicate of '%0'. Keeping '%0'", "report%1.pdf", "b")),
                Arguments.of("a%0 mode", new LocalizationKeyParams("%0 mode", "a%0")),
                Arguments.of("%1 then %0", new LocalizationKeyParams("%0 then %1", "%1", "%0"))
        );
    }

    @Test
    void tooManyParams() {
        assertThrows(IllegalStateException.class, () -> new LocalizationKeyParams("", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"));
    }
}
