package org.jabref.logic.formatter.bibtexfields;

import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.Formatter;

public class RegexFormatter implements Formatter {

    private static String[] rex;

    @Override
    public String getName() {
        return Localization.lang("Regex");
    }

    @Override
    public String getKey() {
        return "regex";
    }

    @Override
    public String format(String oldString) {
        Objects.requireNonNull(oldString);
        rex[0] = rex[0].replaceAll("\"", "");
        rex[1] = rex[1].replaceAll("\"", "");

        return oldString.replaceAll(rex[0], rex[1]);
    }

    @Override
    public String getDescription() {
        return Localization.lang("Add a regular expression for the key pattern.");
    }

    @Override
    public String getExampleInput() {
        return "Please replace the spaces";
    }

    public static void setRegex(String regex) {
        regex = regex.substring(1, regex.length() - 1);
        String[] parts = regex.split("\",\"");
        parts[0] += "\"";
        parts[1] = "\"" + parts[1];
        rex = parts;
    }

}
