package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

public class EscapeAmpersandsFormatter extends Formatter {

    @Override
    public String getName() {
        return Localization.lang("Escape ampersands");
    }

    @Override
    public String getKey() {
        return "escapeAmpersands";
    }

    @Override
    public String format(String value) {
        Objects.requireNonNull(value);

        StringBuilder result = new StringBuilder();

        boolean escape = false;
        boolean inCommandName = false;
        boolean inCommand = false;
        boolean inCommandOption = false;
        int nestedEnvironments = 0;
        StringBuilder commandName = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            // Track whether we are in a LaTeX command of some sort.
            if (Character.isLetter(c) && (escape || inCommandName)) {
                inCommandName = true;
                if (!inCommandOption) {
                    commandName.append(c);
                }
            } else if (Character.isWhitespace(c) && (inCommand || inCommandOption)) {
                // Whitespace
            } else if (inCommandName) {
                // This means the command name is ended.
                // Perhaps the beginning of an argument:
                if (c == '[') {
                    inCommandOption = true;
                } else if (inCommandOption && (c == ']')) {
                    // Or the end of an argument:
                    inCommandOption = false;
                } else if (!inCommandOption && (c == '{')) {
                    inCommandName = false;
                    inCommand = true;
                } else {
                    // Or simply the end of this command alltogether:
                    commandName.delete(0, commandName.length());
                    inCommandName = false;
                }
            }
            // If we are in a command body, see if it has ended:
            if (inCommand && (c == '}')) {
                if ("begin".equals(commandName.toString())) {
                    nestedEnvironments++;
                }
                if ((nestedEnvironments > 0) && "end".equals(commandName.toString())) {
                    nestedEnvironments--;
                }

                commandName.delete(0, commandName.length());
                inCommand = false;
            }

            // We add a backslash before any ampersand characters, with one exception: if
            // we are inside an \\url{...} command, we should write it as it is. Maybe.
            if ((c == '&') && !escape && !(inCommand && "url".equals(commandName.toString()))
                    && (nestedEnvironments == 0)) {
                result.append("\\&");
            } else {
                result.append(c);
            }
            escape = c == '\\';
        }
        return result.toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Escape ampersands");
    }

    @Override
    public String getExampleInput() {
        return "Text & with &ampersands";
    }
}
