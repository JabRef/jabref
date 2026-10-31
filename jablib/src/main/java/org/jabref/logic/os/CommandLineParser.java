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

    public static List<String> withDirectory(List<String> arguments, String directory) {
        String replacement = Matcher.quoteReplacement(directory);
        return arguments.stream()
                        .map(argument -> DIRECTORY_PLACEHOLDER.matcher(argument).replaceAll(replacement))
                        .toList();
    }

    public static List<String> splitLegacyCommandLine(String commandLine) {
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
            } else if (c == '\'' && !inDoubleQuotes && !isEmbeddedApostrophe(commandLine, i)) {
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

        if (inSingleQuotes || inDoubleQuotes) {
            return List.of();
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
        return character == '\'' || character == '"' || Character.isWhitespace(character);
    }

    private static boolean isEmbeddedApostrophe(String commandLine, int index) {
        return index > 0 && index + 1 < commandLine.length()
                && Character.isUnicodeIdentifierPart(commandLine.charAt(index - 1))
                && Character.isUnicodeIdentifierPart(commandLine.charAt(index + 1));
    }
}
