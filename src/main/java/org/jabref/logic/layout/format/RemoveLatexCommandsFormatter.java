package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.strings.StringUtil;

public class RemoveLatexCommandsFormatter implements LayoutFormatter {

    @Override
    public String format(String field) {
        StringBuilder cleanedField = new StringBuilder();
        StringBuilder currentCommand = null;
        char currentCharacter;
        boolean escaped = false;
        boolean incommand = false;
        int currentFieldPosition;
        for (currentFieldPosition = 0; currentFieldPosition < field.length(); currentFieldPosition++) {
            currentCharacter = field.charAt(currentFieldPosition);
            if (escaped && (currentCharacter == '\\')) {
                cleanedField.append('\\');
                escaped = false;
                // \\ --> first \ begins the command, second \ ends the command
                // \latexommand\\ -> \latexcommand is the command, terminated by \, which begins a new command
                incommand = false;
            } else if (currentCharacter == '\\') {
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((currentCharacter == '{') || (currentCharacter == '}'))) {
                // Swallow the brace.
            } else if (Character.isLetter(currentCharacter) || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(currentCharacter))) {
                escaped = false;
                if (incommand) {
                    currentCommand.append(currentCharacter);
                    if ((currentCommand.length() == 1)
                            && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        // This indicates that we are in a command of the type \^o or \~{n}
                        incommand = false;
                        escaped = false;
                    }
                } else {
                    cleanedField.append(currentCharacter);
                }
            } else if (Character.isLetter(currentCharacter)) {
                escaped = false;
                if (incommand) {
                    // We are in a command, and should not keep the letter.
                    currentCommand.append(currentCharacter);
                } else {
                    cleanedField.append(currentCharacter);
                }
            } else {
                if (!incommand || (!Character.isWhitespace(currentCharacter) && (currentCharacter != '{'))) {
                    cleanedField.append(currentCharacter);
                } else {
                    if (!Character.isWhitespace(currentCharacter) && (currentCharacter != '{')) {
                        // do not append the opening brace of a command parameter
                        // do not append the whitespace character
                        cleanedField.append(currentCharacter);
                    }
                    if (incommand) {
                        // eat up all whitespace characters
                        while ((currentFieldPosition + 1 < field.length() && Character.isWhitespace(field.charAt(currentFieldPosition + 1)))) {
                            currentFieldPosition++;
                        }
                    }
                }
                incommand = false;
                escaped = false;
            }
        }

        return cleanedField.toString();
    }
}
