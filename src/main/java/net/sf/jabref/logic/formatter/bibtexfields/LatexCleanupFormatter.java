package net.sf.jabref.logic.formatter.bibtexfields;

import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

public class LatexCleanupFormatter implements Formatter {

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
        newValue = newValue.replace("$$", "").replaceAll("(?<!\\\\[\\p{Alpha}]{0,100}\\{[^\\}]{0,100})\\}([-/ ]?)\\{",
                "$1");
        // Move numbers, +, -, /, and brackets into equations
        newValue = newValue.replaceAll("(([^$]|\\\\\\$)*)\\$", "$1@@"); // Replace $, but not \$ with @@
        newValue = newValue.replaceAll("([^@]*)@@([^@]*)@@", "$1\\$$2@@"); // Replace every other @@ with $
        //newValue = newValue.replaceAll("([0-9\\(\\.]+) \\$","\\$$1\\\\ "); // Move numbers followed by a space left of $ inside the equation, e.g., 0.35 $\mu$m
        newValue = newValue.replaceAll("([0-9\\(\\.]+[ ]?[-+/]?[ ]?)\\$", "\\$$1"); // Move numbers, possibly with operators +, -, or /,  left of $ into the equation
        newValue = newValue.replaceAll("@@([ ]?[-+/]?[ ]?[0-9\\)\\.]+)", " $1@@"); // Move numbers right of @@ into the equation
        newValue = newValue.replace("@@", "$"); // Replace all @@ with $
        newValue = newValue.replace("  ", " "); // Clean up
        newValue = newValue.replace("$$", "");
        newValue = newValue.replace(" )$", ")$");
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
