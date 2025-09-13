package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnicodeToLatexFormatterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    private static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of("", ""), // empty string input
                Arguments.of("abc", "abc"), // non unicode input
                Arguments.of("{{\\aa}}{\\\"{a}}{\\\"{o}}", "\u00E5\u00E4\u00F6"), // multiple unicodes input
                Arguments.of("", "\u0081"), // high code point unicode, boundary case: cp = 129
                Arguments.of("", "\u0080"), // high code point unicode, boundary case: cp = 128 < 129
                Arguments.of("M{\\\"{o}}nch", new UnicodeToLatexFormatter().getExampleInput()),
                Arguments.of("{\\i}", "ı"),
                Arguments.of("{\\i} {\\={\\i}}", "ı ī"),
                Arguments.of("{\\={\\i}}{\\={\\i}}", "\u0069\u0304\u0069\u0304"),
                Arguments.of("Pu{\\d{n}}ya-pattana-vidy{\\={a}}-p{\\={\\i}}{\\d{t}}h{\\={a}}dhi-k{\\d{r}}tai{\\d{h}} pr{\\={a}}-ka{{\\'{s}}}ya{\\d{m}} n{\\={\\i}}ta{\\d{h}}", "Puṇya-pattana-vidyā-pīṭhādhi-kṛtaiḥ prā-kaśyaṃ nītaḥ")
        );
    }

    @ParameterizedTest()
    @MethodSource("testCases")
    void format(String expectedResult, String input) {
        assertEquals(expectedResult, formatter.format(input));
    }
}
