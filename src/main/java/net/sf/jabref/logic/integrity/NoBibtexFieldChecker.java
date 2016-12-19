package net.sf.jabref.logic.integrity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.InternalBibtexFields;

/**
 * This checker checks whether the entry does not contain any field appearing only in BibLaTeX (and not in BibTeX)
 */
public class NoBibtexFieldChecker implements Checker {

    private List<String> getAllBiblatexOnlyFields() {
        Set<String> allBibtexFields = BibtexEntryTypes.ALL.stream().flatMap(type -> type.getAllFields().stream()).collect(Collectors.toSet());
        return BibLatexEntryTypes.ALL.stream()
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
                .map(name -> new IntegrityMessage(Localization.lang("BibLaTeX field only"), entry, name)).collect(Collectors.toList());
    }

}
