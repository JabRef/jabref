package net.sf.jabref.logic.integrity;

import java.util.Optional;

import net.sf.jabref.logic.l10n.Localization;

public class PersonNamesChecker implements ValueChecker {

    @Override
    public Optional<String> checkValue(String value) {
        String valueTrimmedAndLowerCase = value.trim().toLowerCase();
        if (valueTrimmedAndLowerCase.startsWith("and ") || valueTrimmedAndLowerCase.startsWith(",")) {
            return Optional.of(Localization.lang("should start with a name"));
        } else if (valueTrimmedAndLowerCase.endsWith(" and") || valueTrimmedAndLowerCase.endsWith(",")) {
            return Optional.of(Localization.lang("should end with a name"));
        }
        return Optional.empty();
    }
}
