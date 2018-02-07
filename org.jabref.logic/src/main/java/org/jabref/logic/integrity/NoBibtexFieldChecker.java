package org.jabref.logic.integrity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.integrity.IntegrityCheck.Checker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.InternalBibtexFields;

/**
 * This checker checks whether the entry does not contain any field appearing only in biblatex (and not in BibTeX)
 */
public class NoBibtexFieldChecker implements Checker {

    private List<String> getAllBiblatexOnlyFields() {
        Set<String> allBibtexFields = BibtexEntryTypes.ALL.stream().flatMap(type -> type.getAllFields().stream()).collect(Collectors.toSet());
        return BiblatexEntryTypes.ALL.stream()
                .flatMap(type -> type.getAllFields().stream())
                .filter(fieldName -> !allBibtexFields.contains(fieldName))
                // these fields are displayed by JabRef as default
                .filter(fieldName -> !InternalBibtexFields.DEFAULT_GENERAL_FIELDS.contains(fieldName))
                .filter(fieldName -> !fieldName.equals(FieldName.ABSTRACT))
                .filter(fieldName -> !fieldName.equals(FieldName.REVIEW))
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        // non-static initalization of ALL_BIBLATEX_ONLY_FIELDS as the user can customize the entry types during runtime
        final List<String> allBiblatexOnlyFields = getAllBiblatexOnlyFields();
        return entry.getFieldNames().stream()
                .filter(name ->  allBiblatexOnlyFields.contains(name))
                .map(name -> new IntegrityMessage(Localization.lang("biblatex field only"), entry, name)).collect(Collectors.toList());
    }

}
