package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.strings.StringUtil;

public class RemoveLatexCommandsFormatter implements LayoutFormatter {

    @Override
    public String format(String field) {
        StringBuilder sb = new StringBuilder("");
        StringBuilder currentCommand = null;
        char c;
        boolean escaped = false;
        boolean incommand = false;
        int i;
        for (i = 0; i < field.length(); i++) {
            c = field.charAt(i);
            if (escaped && (c == '\\')) {
                sb.append('\\');
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
                incommand = true;
                currentCommand = new StringBuilder();
            } else if (!incommand && ((c == '{') || (c == '}'))) {
                // Swallow the brace.
            } else if (Character.isLetter(c) || StringUtil.SPECIAL_COMMAND_CHARS.contains(String.valueOf(c))) {
                escaped = false;
                if (incommand) {
                    currentCommand.append(c);
                    if ((currentCommand.length() == 1)
                            && StringUtil.SPECIAL_COMMAND_CHARS.contains(currentCommand.toString())) {
                        // This indicates that we are in a command of the type \^o or \~{n}
                        incommand = false;
                        escaped = false;

                    }
                } else {
                    sb.append(c);
                }
            } else if (Character.isLetter(c)) {
                escaped = false;
                if (incommand) {
                    // We are in a command, and should not keep the letter.
                    currentCommand.append(c);
                } else {
                    sb.append(c);
                }
            } else {
                if (!incommand || (!Character.isWhitespace(c) && (c != '{'))) {
                    sb.append(c);
                } else {
                    if (c != '{') {
                        sb.append(c);
                    }
                }
                incommand = false;
                escaped = false;
            }
        }

        return sb.toString();
    }

}
