package org.jabref.logic.layout.format;

import java.util.List;

import org.jabref.logic.layout.AbstractParamLayoutFormatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractParamLayoutFormatterTest {
    static class ParseArgumentTester extends AbstractParamLayoutFormatter {
        public static List<String> callParseArgument(String arg) {
            return parseArgument(arg);
        }

        @Override
        public String format(String fieldText) {
            return null;
        }

        @Override
        public void setArgument(String arg) {
        }
    }

    @Test
    void simpleArguments() {
        String input = "arg1,arg2,arg3";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of("arg1", "arg2", "arg3"), result,
                "Simple arguments should be split correctly by commas");
    }

    @Test
    void escapedCommas() {
        String input = "arg1\\,arg2,arg3";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of("arg1,arg2", "arg3"), result,
                "Escaped commas should be treated as literal commas");
    }

    @Test
    void escapedBackslash() {
        String input = "arg1\\\\,arg2";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of("arg1\\", "arg2"), result, "Escaped backslashes should be treated correctly");
    }

    @Test
    void emptyArgument() {
        String input = "";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of(""), result, "Empty string should return a list with an empty string");
    }

    @Test
    void singleArgument() {
        String input = "singleArg";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of("singleArg"), result,
                "Single argument should return a list with the argument itself");
    }

    @Test
    void newlineAndTabEscapeSequences() {
        String input = "arg1\\narg2\\targ3";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of("arg1\narg2\targ3"), result,
                "Escape sequences like newline and tab should be handled correctly");
    }

    @Test
    void multipleEscapedCharacters() {
        String input = "arg1\\n,arg2\\t,arg3\\,arg4";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of("arg1\n", "arg2\t", "arg3,arg4"), result,
                "Multiple escaped characters should be handled correctly");
    }

    @Test
    void mixedCases() {
        String input = "arg1,arg2\\,withComma,arg3\\nnewline,arg4\\\\backslash";
        List<String> result = ParseArgumentTester.callParseArgument(input);
        assertEquals(List.of("arg1", "arg2,withComma", "arg3\nnewline", "arg4\\backslash"), result,
                "Mixed cases should be parsed correctly");
    }
}
