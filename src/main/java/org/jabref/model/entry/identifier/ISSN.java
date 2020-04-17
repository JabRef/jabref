package org.jabref.model.entry.identifier;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ISSN {

    private static final Pattern ISSN_PATTERN = Pattern.compile("^\\d{4}-\\d{3}[\\dxX]$");
    private static final Pattern ISSN_PATTERN_NODASH = Pattern.compile("^(\\d{4})(\\d{3}[\\dxX])$");

    private final String issnString;

    public ISSN(String issnString) {
        this.issnString = Objects.requireNonNull(issnString).trim();
    }

    public boolean isValidFormat() {
        Matcher issnMatcher = ISSN_PATTERN.matcher(issnString);
        return (issnMatcher.matches());
    }

    public boolean isCanBeCleaned() {
        Matcher issnNoDashMatcher = ISSN_PATTERN_NODASH.matcher(issnString);
        return (issnNoDashMatcher.matches());
    }

    public String getCleanedISSN() {
        Matcher issnNoDashMatcher = ISSN_PATTERN_NODASH.matcher(issnString);
        if (issnNoDashMatcher.find()) {
            return issnNoDashMatcher.replaceFirst("$1-$2");
        }
        return issnString;
    }

    public boolean isValidChecksum() {
        // Check that the control digit is correct, see e.g. https://en.wikipedia.org/wiki/International_Standard_Serial_Number
        int sum = 0;
        for (int pos = 0; pos <= 7; pos++) {
            char c = issnString.charAt(pos);
            if (pos != 4) {
                sum += (c - '0') * ((8 - pos) + (pos > 4 ? 1 : 0));
            }
        }
        char control = issnString.charAt(8);
        if ((control == 'x') || (control == 'X')) {
            control = '9' + 1;
        }
        return (((((sum % 11) + control) - '0') == 11) || ((sum % 11) == 0));
    }
}
