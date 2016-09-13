package net.sf.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldProperty;
import net.sf.jabref.model.entry.InternalBibtexFields;

public class AuthorNameChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();
        for (String field : entry.getFieldNames()) {
            if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.PERSON_NAMES)) {
                Optional<String> value = entry.getField(field);
                if (!value.isPresent()) {
                    return Collections.emptyList();
                }

                String valueTrimmedAndLowerCase = value.get().trim().toLowerCase();
                if (valueTrimmedAndLowerCase.startsWith("and ") || valueTrimmedAndLowerCase.startsWith(",")) {
                    result.add(new IntegrityMessage(Localization.lang("should start with a name"), entry, field));
                } else if (valueTrimmedAndLowerCase.endsWith(" and") || valueTrimmedAndLowerCase.endsWith(",")) {
                    result.add(new IntegrityMessage(Localization.lang("should end with a name"), entry, field));
                }
            }
        }
        return result;
    }
}
