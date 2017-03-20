package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

public class RegexFormatter implements Formatter {

    private static String[] regex;

    @Override
    public String getName() {
        return Localization.lang("Regex");
    }

    @Override
    public String getKey() {
        return "regex";
    }

    @Override
    public String format(String input) {
        if (regex == null) {
            return input;
        }
        return input.replaceAll(regex[0], regex[1]);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Add a regular expression for the key pattern.");
    }

    @Override
    public String getExampleInput() {
        return "Please replace the spaces";
    }

    public static void setRegex(String rex) {
        // formatting is like ("exp1","exp2"), we want to remove (" and ")
        rex = rex.substring(2, rex.length() - 2);
        String[] parts = rex.split("\",\"");
        regex = parts;
    }

}
