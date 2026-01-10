package org.jabref.logic.journals.quality.checkers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jabref.logic.journals.quality.AbbreviationEntry;
import org.jabref.logic.journals.quality.Checker;
import org.jabref.logic.journals.quality.Finding;
import org.jabref.logic.journals.quality.Severity;

/**
 * Detects suspicious escape sequences in journal name or abbreviations.
 */
public class WrongEscapeChecker implements Checker {

    // Regex: matches any backslash (escaped as \\\\ in Java)
    private static final Pattern ESCAPE_PATTERN = Pattern.compile(".*\\\\.*");

    @Override
    public List<Finding> check(List<AbbreviationEntry> entries) {
        List<Finding> findings = new ArrayList<>();
        for (AbbreviationEntry entry : entries) {
            if (ESCAPE_PATTERN.matcher(entry.full()).matches()
                    || ESCAPE_PATTERN.matcher(entry.abbr()).matches()) {
                findings.add(new Finding(
                        Severity.ERROR,
                        code(),
                        "Suspicious escape sequence found",
                        entry));
            }
        }
        return findings;
    }

    @Override
    public String code() {
        return "ERR_WRONG_ESCAPE";
    }
}
