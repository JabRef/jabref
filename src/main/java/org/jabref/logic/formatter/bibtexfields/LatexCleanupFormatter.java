package org.jabref.logic.formatter.bibtexfields;

import java.util.regex.Pattern;

import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.l10n.Localization;

/**
 * Simplifies LaTeX syntax. {@see org.jabref.logic.layout.format.RemoveLatexCommandsFormatter} for a formatter removing LaTeX commands completely.
 */
public class LatexCleanupFormatter extends Formatter {

    private static final Pattern REMOVE_REDUNDANT = Pattern
            .compile("(?<!\\\\[\\p{Alpha}]{0,100}\\{[^\\}]{0,100})\\}([-/ ]?)\\{");

    private static final Pattern REPLACE_WITH_AT = Pattern.compile("(^|[^\\\\$])\\$");
    private static final Pattern REPLACE_EVERY_OTHER_AT = Pattern.compile("([^@]*)@@([^@]*)@@");
    private static final Pattern MOVE_NUMBERS_WITH_OPERATORS = Pattern.compile("([0-9\\(\\.]+[ ]?[-+/]?[ ]?)\\$");
    private static final Pattern MOVE_NUMBERS_RIGHT_INTO_EQUATION = Pattern.compile("@@([ ]?[-+/]?[ ]?[0-9\\)\\.]+)");
    private static final Pattern ESCAPE_PERCENT_SIGN_ONCE = Pattern.compile("(^|[^\\\\%])%");

    @Override
    public String getName() {
        return Localization.lang("LaTeX cleanup");
    }

    @Override
    public String getKey() {
        return "latex_cleanup";
    }

    @Override
    public String format(String oldString) {
        String newValue = oldString;

        // Remove redundant $, {, and }, but not if the } is part of a command argument: \mbox{-}{GPS} should not be adjusted
        newValue = newValue.replace("$$", "");
        newValue = REMOVE_REDUNDANT.matcher(newValue).replaceAll("$1");

        // Move numbers, +, -, /, and brackets into equations
        newValue = REPLACE_WITH_AT.matcher(newValue).replaceAll("$1@@"); // Replace $, but not \$ with @@

        newValue = REPLACE_EVERY_OTHER_AT.matcher(newValue).replaceAll("$1\\$$2@@"); // Replace every other @@ with $
        // newValue = newValue.replaceAll("([0-9\\(\\.]+) \\$","\\$$1\\\\ "); // Move numbers followed by a space left of $ inside the equation, e.g., 0.35 $\mu$m

        newValue = MOVE_NUMBERS_WITH_OPERATORS.matcher(newValue).replaceAll("\\$$1"); // Move numbers, possibly with operators +, -, or /,  left of $ into the equation
        newValue = MOVE_NUMBERS_RIGHT_INTO_EQUATION.matcher(newValue).replaceAll(" $1@@"); // Move numbers right of @@ into the equation

        newValue = newValue.replace("@@", "$"); // Replace all @@ with $
        newValue = newValue.replace("  ", " "); // Clean up
        newValue = newValue.replace("$$", "");
        newValue = newValue.replace(" )$", ")$");

        newValue = ESCAPE_PERCENT_SIGN_ONCE.matcher(newValue).replaceAll("$1\\\\%"); // escape %, but do not escapee \% again,  used for comments in TeX

        return newValue;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Cleans up LaTeX code.");
    }

    @Override
    public String getExampleInput() {
        return "{VLSI} {DSP}";
    }
}
