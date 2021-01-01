package org.jabref.logic.l10n;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocalizationParserTest {

    public static Stream<Arguments> singleLineChecks() {
        return Stream.of(
                Arguments.of("Localization.lang(\"one per line\")", "one\\ per\\ line"),
                Arguments.of("Localization.lang(\n            \"Copy \\\\cite{citation key}\")", "Copy\\ \\cite{citation\\ key}"),
                Arguments.of("Localization.lang(\"multi \" + \n\"line\")", "multi\\ line"),
                Arguments.of("Localization.lang(\"one per line with var\", var)", "one\\ per\\ line\\ with\\ var"),
                Arguments.of("Localization.lang(\"Search %0\", \"Springer\")", "Search\\ %0"),
                Arguments.of("Localization.lang(\"Reset preferences (key1,key2,... or 'all')\")", "Reset\\ preferences\\ (key1,key2,...\\ or\\ 'all')"),
                Arguments.of("Localization.lang(\"Multiple entries selected. Do you want to change the type of all these to '%0'?\")",
                        "Multiple\\ entries\\ selected.\\ Do\\ you\\ want\\ to\\ change\\ the\\ type\\ of\\ all\\ these\\ to\\ '%0'?"),
                Arguments.of("Localization.lang(\"Run fetcher, e.g. \\\"--fetch=Medline:cancer\\\"\");",
                        "Run\\ fetcher,\\ e.g.\\ \"--fetch\\=Medline\\:cancer\"")

        );
    }

    public static Stream<Arguments> multiLineChecks() {
        return Stream.of(
                Arguments.of("Localization.lang(\"two per line\") Localization.lang(\"two per line\")", Arrays.asList("two\\ per\\ line", "two\\ per\\ line"))
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
                "Localization.lang(\"Newline\nthere\")",
                "Localization.lang(\"Newline\\nthere\")"
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
        List<String> languageKeysInString = LocalizationParser.JavaLocalizationEntryParser.getLanguageKeysInString(code, LocalizationBundleForTest.LANG);
        assertEquals(expectedLanguageKeys, languageKeysInString);
    }

    @ParameterizedTest
    @MethodSource("singleLineParameterChecks")
    public void testLocalizationParameterParsing(String code, String expectedParameter) {
        List<String> languageKeysInString = LocalizationParser.JavaLocalizationEntryParser.getLocalizationParameter(code, LocalizationBundleForTest.LANG);
        assertEquals(List.of(expectedParameter), languageKeysInString);
    }

    @ParameterizedTest
    @MethodSource("causesRuntimeExceptions")
    public void throwsRuntimeException(String code) {
        assertThrows(RuntimeException.class, () -> LocalizationParser.JavaLocalizationEntryParser.getLanguageKeysInString(code, LocalizationBundleForTest.LANG));
    }
}
