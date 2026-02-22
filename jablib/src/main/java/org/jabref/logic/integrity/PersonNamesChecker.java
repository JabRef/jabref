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

        String valueTrimmed = value.trim();
        String valueLower = valueTrimmed.toLowerCase(Locale.ROOT);

        if (valueLower.startsWith("and ") || valueLower.startsWith(",")) {
            return Optional.of(Localization.lang("should start with a name"));
        } else if (valueLower.endsWith(" and") || valueLower.endsWith(",")) {
            return Optional.of(Localization.lang("should end with a name"));
        }

        if (valueTrimmed.startsWith("{") && valueTrimmed.endsWith("}")) {
            return Optional.empty();
        }

        String valueWithoutBrackets = new RemoveBrackets().format(valueTrimmed);
        AuthorList authorList = AuthorList.parse(valueTrimmed);

        if (!authorList.getAsLastFirstNamesWithAnd(false).equals(valueWithoutBrackets)
                && !authorList.getAsFirstLastNamesWithAnd().equals(valueWithoutBrackets)) {
            return Optional.of(Localization.lang("Names are not in the standard %0 format.", bibMode.getFormattedName()));
        }

        return Optional.empty();
    }
}
