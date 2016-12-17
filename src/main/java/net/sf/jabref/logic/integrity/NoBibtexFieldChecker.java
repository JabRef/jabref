package net.sf.jabref.logic.integrity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;

/**
 * This checker checks whether the entry does not contain any field appearing only in BibLaTeX (and not in BibTex)
 */
public class NoBibtexFieldChecker implements Checker {

    private static final List<String> ALL_BIBLATEX_ONLY_FIELDS;

    static {
        Set<String> allBibtexFields = BibtexEntryTypes.ALL.stream().flatMap(type -> type.getAllFields().stream()).collect(Collectors.toSet());
        ALL_BIBLATEX_ONLY_FIELDS = BibLatexEntryTypes.ALL.stream().flatMap(type -> type.getAllFields().stream()).filter(fieldName -> !allBibtexFields.contains(fieldName)).sorted().collect(Collectors.toList());
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldNames().stream()
                .filter(name ->  ALL_BIBLATEX_ONLY_FIELDS.contains(name))
                .map(name -> new IntegrityMessage(Localization.lang("BibLaTeX field only"), entry, name)).collect(Collectors.toList());
    }

}
