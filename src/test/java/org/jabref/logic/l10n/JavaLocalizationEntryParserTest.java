package org.jabref.logic.l10n;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JavaLocalizationEntryParserTest {
    public static Stream<Arguments> singleLineChecks() {
        return Stream.of(
                Arguments.of("Localization.lang(\"one per line\")", "one per line"),

                // '\c' is an escaped character, thus "\cite" is wrong as lookup text. It has to be "\\cite" in the .properties file
                Arguments.of("Localization.lang(\"Copy \\\\cite{citation key}\")", "Copy \\\\cite{citation key}"),

                // " is kept unescaped
                Arguments.of("Localization.lang(\"\\\"Hey\\\"\")", "\"Hey\""),

                // \n is a "real" newline character in the simulated read read Java source code
                Arguments.of("Localization.lang(\"multi \" + \n\"line\")", "multi line"),
                Arguments.of("Localization.lang(\n            \"A string\")", "A string"),

                Arguments.of("Localization.lang(\"one per line with var\", var)", "one per line with var"),
                Arguments.of("Localization.lang(\"Search %0\", \"Springer\")", "Search %0"),
                Arguments.of("Localization.lang(\"Reset preferences (key1,key2,... or 'all')\")", "Reset preferences (key1,key2,... or 'all')"),
                Arguments.of("Localization.lang(\"Multiple entries selected. Do you want to change the type of all these to '%0'?\")",
                        "Multiple entries selected. Do you want to change the type of all these to '%0'?"),
                Arguments.of("Localization.lang(\"Run fetcher, e.g. \\\"--fetch=Medline:cancer\\\"\");",
                        "Run fetcher, e.g. \"--fetch=Medline:cancer\""),

                // \n is allowed. See // see also https://stackoverflow.com/a/10285687/873282
                // It appears as "\n" literally in the source code
                // It appears as "\n" as key of the localization, too
                // To mirror that, we have to write \\n here
                Arguments.of("Localization.lang(\"First line\\nSecond line\")", "First line\\nSecond line")
        );
    }

    public static Stream<Arguments> multiLineChecks() {
        return Stream.of(
                Arguments.of("Localization.lang(\"two per line\") Localization.lang(\"two per line\")", Arrays.asList("two per line", "two per line"))
        );
    }

    public static Stream<Arguments> singleLineParameterChecks() {
        return Stream.of(
                Arguments.of("Localization.lang(\"one per line\")", "\"one per line\""),
                Arguments.of("Localization.lang(\"one per line\" + var)", "\"one per line\" + var"),
                Arguments.of("Localization.lang(var + \"one per line\")", "var + \"one per line\""),
                Arguments.of("Localization.lang(\"Search %0\", \"Springer\")", "\"Search %0\", \"Springer\"")
        );
    }

    public static Stream<String> causesRuntimeExceptions() {
        return Stream.of(
                "Localization.lang(\"Ends with a space \")",
                // "\\n" in the *.java source file
                "Localization.lang(\"Escaped newline\\\\nthere\")"
        );
    }

    @ParameterizedTest
    @MethodSource("singleLineChecks")
    public void testLocalizationKeyParsing(String code, String expectedLanguageKeys) {
        testLocalizationKeyParsing(code, List.of(expectedLanguageKeys));
    }

    @ParameterizedTest
    @MethodSource("multiLineChecks")
    public void testLocalizationKeyParsing(String code, List<String> expectedLanguageKeys) {
        List<String> languageKeysInString = JavaLocalizationEntryParser.getLanguageKeysInString(code, LocalizationBundleForTest.LANG);
        assertEquals(expectedLanguageKeys, languageKeysInString);
    }

    @ParameterizedTest
    @MethodSource("singleLineParameterChecks")
    public void testLocalizationParameterParsing(String code, String expectedParameter) {
        List<String> languageKeysInString = JavaLocalizationEntryParser.getLocalizationParameter(code, LocalizationBundleForTest.LANG);
        assertEquals(List.of(expectedParameter), languageKeysInString);
    }

    @ParameterizedTest
    @MethodSource("causesRuntimeExceptions")
    public void throwsRuntimeException(String code) {
        assertThrows(RuntimeException.class, () -> JavaLocalizationEntryParser.getLanguageKeysInString(code, LocalizationBundleForTest.LANG));
    }
}
