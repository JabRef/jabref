package org.jabref.logic.integrity;

import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.RemoveBrackets;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.AuthorList;

public class PersonNamesChecker implements ValueChecker {

    private final BibDatabaseMode bibMode;

    public PersonNamesChecker(BibDatabaseContext databaseContext) {
        this.bibMode = databaseContext.getMode();
    }

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

        // Check format with brackets first to support protected institutional authors
        if (isStandardFormat(value)) {
            return Optional.empty();
        }

        // Remove all brackets to handle corporate names correctly, e.g., {JabRef}
        String valueWithoutBrackets = new RemoveBrackets().format(value);

        // Check that the value is in one of the two standard BibTeX formats:
        //  Last, First and ...
        //  First Last and ...
        if (!isStandardFormat(valueWithoutBrackets)) {
            return Optional.of(Localization.lang("Names are not in the standard %0 format.", bibMode.getFormattedName()));
        }

        return Optional.empty();
    }

    private boolean isStandardFormat(String name) {
        AuthorList authorList = AuthorList.parse(name);
        return authorList.getAsLastFirstNamesWithAnd(false).equals(name)
                || authorList.getAsFirstLastNamesWithAnd().equals(name);
    }
}
