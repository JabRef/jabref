package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;


public class ISBNChecker implements Checker {

    private static final Pattern ISBN_PATTERN = Pattern.compile("^(\\d{9}[\\dxX]|\\d{13})$");

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (!entry.hasField("isbn")) {
            return Collections.emptyList();
        }

        // Check that the ISSN is on the correct form
        String isbn = entry.getFieldOptional("isbn").get().trim().replace("-", "");
        Matcher isbnMatcher = ISBN_PATTERN.matcher(isbn);

        if (!isbnMatcher.matches()) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("incorrect format"), entry, "isbn"));
        }

        boolean valid;
        if (isbn.length() == 10) {
            valid = isbn10check(isbn);
        } else {
            // length is either 10 or 13 based on regexp so will be 13 here
            valid = isbn13check(isbn);
        }
        if (valid) {
            return Collections.emptyList();
        } else {
            return Collections
                    .singletonList(new IntegrityMessage(Localization.lang("incorrect control digit"), entry, "isbn"));
        }
    }

    // Check that the control digit is correct, see e.g. https://en.wikipedia.org/wiki/International_Standard_Book_Number#Check_digits
    private boolean isbn10check(String isbn) {
        int sum = 0;
        for (int pos = 0; pos <= 8; pos++) {
            sum += (isbn.charAt(pos) - '0') * ((10 - pos));
        }
        char control = isbn.charAt(9);
        if ((control == 'x') || (control == 'X')) {
            control = '9' + 1;
        }
        sum += (control - '0');
        return (sum % 11) == 0;
    }

    // Check that the control digit is correct, see e.g. https://en.wikipedia.org/wiki/International_Standard_Book_Number#Check_digits
    private boolean isbn13check(String isbn) {
        int sum = 0;
        for (int pos = 0; pos <= 12; pos++) {
            sum += (isbn.charAt(pos) - '0') * ((pos % 2) == 0 ? 1 : 3);
        }
        return (sum % 10) == 0;
    }
}
