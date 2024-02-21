package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.strings.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class RemoveLatexCommandsFormatter implements LayoutFormatter {
    public static Map<Integer, Boolean> branchCoverage = new HashMap<>();

    @Override
    public String format(String field) {
        StringBuilder cleanedField = new StringBuilder();
        StringBuilder currentCommand = null;
        char currentCharacter;
        boolean escaped = false;
        boolean incommand = false;
        int currentFieldPosition;
        for (currentFieldPosition = 0; currentFieldPosition < field.length(); currentFieldPosition++) {
            branchCoverage.put(1, true);
            currentCharacter = field.charAt(currentFieldPosition);
            if (escaped && (currentCharacter == '\\')) {
                branchCoverage.put(2, true);
                branchCoverage.put(3, true);
                cleanedField.append('\\');
                escaped = false;
                // \\ --> first \ begins the command, second \ ends the command
                // \latexommand\\ -> \latexcommand is the command, terminated by \, which begins a new command
                incommand = false;
            } else if (currentCharacter == '\\') {
                branchCoverage.put(4, true);
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((currentCharacter == '{') || (currentCharacter == '}'))) {
                branchCoverage.put(5, true);
                branchCoverage.put(6, true);
                branchCoverage.put(7, true);

                // Swallow the brace.
            } else if (Character.isLetter(currentCharacter) || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(currentCharacter))) {
                branchCoverage.put(8, true);
                branchCoverage.put(9, true);

                escaped = false;
                if (incommand) {
                    branchCoverage.put(10, true);
                    currentCommand.append(currentCharacter);
                    if ((currentCommand.length() == 1)
                            && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        branchCoverage.put(11, true);
                        branchCoverage.put(12, true);

                        // This indicates that we are in a command of the type \^o or \~{n}
                        incommand = false;
                        escaped = false;
                    }
                } else {
                    branchCoverage.put(13, true);

                    cleanedField.append(currentCharacter);
                }
            } else if (Character.isLetter(currentCharacter)) {
                branchCoverage.put(14, true);

                escaped = false;
                if (incommand) {
                    branchCoverage.put(15, true);

                    // We are in a command, and should not keep the letter.
                    currentCommand.append(currentCharacter);
                } else {
                    branchCoverage.put(16, true);

                    cleanedField.append(currentCharacter);
                }
            } else {
                branchCoverage.put(17, true);

                if (!incommand || (!Character.isWhitespace(currentCharacter) && (currentCharacter != '{'))) {
                    branchCoverage.put(18, true);
                    branchCoverage.put(19, true);
                    branchCoverage.put(20, true);
                    cleanedField.append(currentCharacter);
                } else {
                    branchCoverage.put(21, true);

                    if (!Character.isWhitespace(currentCharacter) && (currentCharacter != '{')) {
                        branchCoverage.put(22, true);
                        branchCoverage.put(23, true);
                        // do not append the opening brace of a command parameter
                        // do not append the whitespace character
                        cleanedField.append(currentCharacter);
                    }
                    if (incommand) {
                        branchCoverage.put(24, true);

                        // eat up all whitespace characters
                        while (currentFieldPosition + 1 < field.length() && Character.isWhitespace(field.charAt(currentFieldPosition + 1))) {
                            branchCoverage.put(25, true);
                            branchCoverage.put(26, true);
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
