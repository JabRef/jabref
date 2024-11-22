package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.strings.StringUtil;

public class RemoveLatexCommandsFormatter implements LayoutFormatter {
    @Override
    public String format(String field) {
        StringBuilder cleanedField = new StringBuilder();
        StringBuilder currentCommand = null;
        boolean escaped = false;
        boolean incommand = false;

        for (int currentFieldPosition = 0; currentFieldPosition < field.length(); currentFieldPosition++) {
            char currentCharacter = field.charAt(currentFieldPosition);

            if (currentCharacter == '\\') {
                // Start of a LaTeX command
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (incommand) {
                // Collect characters for the LaTeX command name
                if (Character.isLetter(currentCharacter) ||
                        StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(currentCharacter))) {
                    currentCommand.append(currentCharacter);
                } else if (currentCharacter == '{') {
                    // Found the start of the command's parameters; skip the command name
                    int braceLevel = 1;
                    currentFieldPosition++; // Move past the opening brace
                    while (currentFieldPosition < field.length() && braceLevel > 0) {
                        currentCharacter = field.charAt(currentFieldPosition);
                        if (currentCharacter == '{') {
                            braceLevel++;
                        } else if (currentCharacter == '}') {
                            braceLevel--;
                        }
                        // Append parameter content if we're still inside the braces
                        if (braceLevel > 0) {
                            cleanedField.append(currentCharacter);
                        }
                        currentFieldPosition++;
                    }
                    incommand = false;
                    escaped = false;
                } else {
                    // End of the command (no parameters)
                    incommand = false;
                    escaped = false;
                }
            } else if (currentCharacter != '{' && currentCharacter != '}') {
                // Append regular characters (non-command, non-braces)
                cleanedField.append(currentCharacter);
            }
        }

        return cleanedField.toString();
    }
}
