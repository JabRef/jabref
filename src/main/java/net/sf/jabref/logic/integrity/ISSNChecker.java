package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;


public class ISSNChecker implements Checker {

    private static final Pattern ISSN_PATTERN = Pattern.compile("^\\d{4}-\\d{3}[\\dxX]$");

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (!entry.hasField("issn")) {
            return Collections.emptyList();
        }

        // Check that the ISSN is on the correct form
        String issn = entry.getField("issn").trim();
        Matcher issnMatcher = ISSN_PATTERN.matcher(issn);
        if (!issnMatcher.matches()) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("incorrect format"), entry, "issn"));
        }

        // Check that the control digit is correct, see e.g. https://en.wikipedia.org/wiki/International_Standard_Serial_Number
        int sum = 0;
        for (int pos = 0; pos <= 7; pos++) {
            char c = issn.charAt(pos);
            if (pos != 4) {
                sum += (c - '0') * ((8 - pos) + (pos > 4 ? 1 : 0));
            }
        }
        char control = issn.charAt(8);
        if ((control == 'x') || (control == 'X')) {
            control = '9' + 1;
        }
        if (((((sum % 11) + control) - '0') == 11) || ((sum % 11) == 0)) {
            return Collections.emptyList();
        } else {
            return Collections
                    .singletonList(new IntegrityMessage(Localization.lang("incorrect control digit"), entry, "issn"));
        }
    }

}
