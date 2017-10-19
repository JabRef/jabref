package org.jabref.logic.integrity;

import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.strings.StringUtil;

public class PersonNamesChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        if (StringUtil.isBlank(value)) {
            return Optional.empty();
        }

        String valueTrimmedAndLowerCase = value.trim().toLowerCase(Locale.ROOT);
        if (valueTrimmedAndLowerCase.startsWith("and ") || valueTrimmedAndLowerCase.startsWith(",")) {
            return Optional.of(Localization.lang("should start with a name"));
        } else if (valueTrimmedAndLowerCase.endsWith(" and") || valueTrimmedAndLowerCase.endsWith(",")) {
            return Optional.of(Localization.lang("should end with a name"));
        }

        // Check that the value is in one of the two standard BibTeX formats:
        //  Last, First and ...
        //  First Last and ...
        AuthorList authorList = AuthorList.parse(value);
        if (!authorList.getAsLastFirstNamesWithAnd(false).equals(value)
                && !authorList.getAsFirstLastNamesWithAnd().equals(value)) {
            return Optional.of(Localization.lang("Names are not in the standard BibTeX format."));
        }

        return Optional.empty();
    }
}
