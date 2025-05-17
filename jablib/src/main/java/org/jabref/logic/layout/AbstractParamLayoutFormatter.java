package org.jabref.logic.layout;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an abstract implementation of ParamLayoutFormatter, which provides some
 * functionality for the handling of argument strings.
 */
public abstract class AbstractParamLayoutFormatter implements ParamLayoutFormatter {

    private static final char SEPARATOR = ',';

    protected AbstractParamLayoutFormatter() {
    }

    /**
     * Parse an argument string and return the parts of the argument. The parts are
     * separated by commas, and escaped commas are reduced to literal commas.
     *
     * @param arg The argument string.
     * @return A list of strings representing the parts of the argument.
     */
    protected static List<String> parseArgument(String arg) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < arg.length(); i++) {
            char currentChar = arg.charAt(i);
            if (escaped) {
                handleEscapedCharacter(current, currentChar);
                escaped = false;
            } else if (currentChar == '\\') {
                escaped = true;
            } else if (currentChar == AbstractParamLayoutFormatter.SEPARATOR) {
                addPart(parts, current);
            } else {
                current.append(currentChar);
            }
        }
        addPart(parts, current);
        return parts;
    }

    private static void handleEscapedCharacter(StringBuilder current, char currentChar) {
        switch (currentChar) {
            case 'n' -> current.append('\n');
            case 't' -> current.append('\t');
            case ',' -> current.append(',');
            case '"' -> current.append('"');
            case '\\' -> current.append('\\');
            default -> {
                current.append('\\');
                current.append(currentChar);
            }
        }
    }

    private static void addPart(List<String> parts, StringBuilder current) {
        parts.add(current.toString());
        current.setLength(0);
    }
}
