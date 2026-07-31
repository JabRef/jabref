package org.jabref.logic.l10n;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ResourceLock("Localization.lang")
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

    /// Code snippets in which `Localization.lang` only occurs inside a comment, hence no key must be found.
    public static Stream<String> commentedOutLocalizations() {
        return Stream.of(
                "// Localization.lang(\"key in line comment\")",
                "/// Localization.lang(\"key in markdown comment\")",
                "int i = 0; // Localization.lang(\"key in trailing line comment\")",
                "/* Localization.lang(\"key in block comment\") */",
                "/*\n * Localization.lang(\"key in multi line block comment\")\n */",
                "/" + "*" + "*" + "\n * Localization.lang(\"key in javadoc\")\n */", // Hide fake javadoc from autoformatter
                // unterminated block comment at the end of a file
                "/* Localization.lang(\"key in unterminated block comment\")"
        );
    }

    public static Stream<Arguments> commentsMixedWithCode() {
        return Stream.of(
                // the comment must not swallow the real key
                Arguments.of("// Localization.lang(\"commented\")\nLocalization.lang(\"real\")", List.of("real")),
                Arguments.of("Localization.lang(\"real\") // Localization.lang(\"commented\")", List.of("real")),
                Arguments.of("/* Localization.lang(\"commented\") */ Localization.lang(\"real\")", List.of("real")),
                // a comment inside the argument list is ignored, the key is still found
                Arguments.of("Localization.lang(\"real\" /* comment */)", List.of("real")),
                Arguments.of("Localization.lang(\"real\" // comment\n)", List.of("real")),
                // "//" inside a string literal does not start a comment
                Arguments.of("String url = \"https://example.org\"; Localization.lang(\"real\")", List.of("real")),
                // a quote inside a comment must not be treated as the start of a string literal
                Arguments.of("// it's commented\nLocalization.lang(\"real\")", List.of("real")),
                Arguments.of("/* \" */ Localization.lang(\"real\")", List.of("real")),
                // character literals containing quotes do not confuse the parser
                Arguments.of("char c = '\"'; Localization.lang(\"real\")", List.of("real")),
                // comment markers inside a text block do not start a comment
                Arguments.of("String s = \"\"\"\n// no comment /* here\n\"\"\"; Localization.lang(\"real\")", List.of("real"))
        );
    }

    @ParameterizedTest
    @MethodSource("commentedOutLocalizations")
    void localizationInCommentIsIgnored(String code) {
        assertEquals(List.of(), JavaLocalizationEntryParser.getLanguageKeysInString(code));
    }

    @ParameterizedTest
    @MethodSource("commentsMixedWithCode")
    void localizationKeysAreFoundNextToComments(String code, List<String> expectedLanguageKeys) {
        assertEquals(expectedLanguageKeys, JavaLocalizationEntryParser.getLanguageKeysInString(code));
    }

    @Test
    void blankOutCommentsKeepsLengthAndLineStructure() {
        String source = "int i = 0; // comment\n/* block\ncomment */ int j = 1;";
        String blanked = JavaLocalizationEntryParser.blankOutComments(source);
        assertEquals(source.length(), blanked.length());
        assertEquals("int i = 0;" + " ".repeat(11) + "\n" + " ".repeat(8) + "\n" + " ".repeat(10) + " int j = 1;", blanked);
    }

    @ParameterizedTest
    @MethodSource("singleLineChecks")
    void localizationKeyParsing(String code, String expectedLanguageKeys) {
        localizationKeyParsing(code, List.of(expectedLanguageKeys));
    }

    @ParameterizedTest
    @MethodSource("multiLineChecks")
    void localizationKeyParsing(String code, List<String> expectedLanguageKeys) {
        List<String> languageKeysInString = JavaLocalizationEntryParser.getLanguageKeysInString(code);
        assertEquals(expectedLanguageKeys, languageKeysInString);
    }

    @ParameterizedTest
    @MethodSource("singleLineParameterChecks")
    void localizationParameterParsing(String code, String expectedParameter) {
        List<String> languageKeysInString = JavaLocalizationEntryParser.getLocalizationParameter(code);
        assertEquals(List.of(expectedParameter), languageKeysInString);
    }

    @ParameterizedTest
    @MethodSource("causesRuntimeExceptions")
    void throwsRuntimeException(String code) {
        assertThrows(RuntimeException.class, () -> JavaLocalizationEntryParser.getLanguageKeysInString(code));
    }
}
