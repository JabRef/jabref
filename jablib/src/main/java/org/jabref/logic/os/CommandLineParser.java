package org.jabref.logic.os;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class CommandLineParser {

    private static final Pattern DIRECTORY_PLACEHOLDER = Pattern.compile("%DIR%?");

    private CommandLineParser() {
    }

    /// Splits a command line into single arguments and replaces the directory placeholder in each of them.
    ///
    /// @param commandLine the command line as entered in the preferences.
    /// @param directory   the directory the command should be started at.
    /// @return the arguments to pass to ProcessBuilder.
    public static List<String> toArguments(String commandLine, String directory) {
        String replacement = Matcher.quoteReplacement(directory);
        return splitCommandLine(commandLine).stream()
                                            .map(argument -> DIRECTORY_PLACEHOLDER.matcher(argument).replaceAll(replacement))
                                            .toList();
    }

    /// Splits a command line string into separate arguments, respecting single and double quotes.
    ///
    /// @param commandLine the command line to format.
    /// @return list of found tokens.
    private static List<String> splitCommandLine(String commandLine) {
        List<String> tokens = new ArrayList<>();
        if (StringUtil.isBlank(commandLine)) {
            return tokens;
        }

        StringBuilder currentToken = new StringBuilder();
        boolean argumentStarted = false;
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;

        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);

            if (c == '\\' && !inSingleQuotes && isEscapable(commandLine, i + 1)) {
                i++;
                currentToken.append(commandLine.charAt(i));
                argumentStarted = true;
            } else if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                argumentStarted = true;
            } else if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                argumentStarted = true;
            } else if (Character.isWhitespace(c) && !inSingleQuotes && !inDoubleQuotes) {
                if (argumentStarted) {
                    tokens.add(currentToken.toString());
                    currentToken.setLength(0);
                    argumentStarted = false;
                }
            } else {
                currentToken.append(c);
                argumentStarted = true;
            }
        }

        if (argumentStarted) {
            tokens.add(currentToken.toString());
        }

        return tokens;
    }

    private static boolean isEscapable(String commandLine, int index) {
        if (index >= commandLine.length()) {
            return false;
        }
        char character = commandLine.charAt(index);
        return character == '\'' || character == '"' || character == '\\' || Character.isWhitespace(character);
    }
}
