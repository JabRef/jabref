package org.jabref.logic.integrity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;

/**
 * This checker checks whether the entry does not contain any field appearing only in biblatex (and not in BibTeX)
 */
public class NoBibtexFieldChecker implements EntryChecker {

    private Set<Field> getAllBiblatexOnlyFields() {
        Set<BibField> allBibtexFields = BibtexEntryTypeDefinitions.ALL.stream().flatMap(type -> type.getAllBibFields().stream()).collect(Collectors.toSet());
        return BiblatexEntryTypeDefinitions.ALL.stream()
                                               .flatMap(type -> type.getAllBibFields().stream())
                                               .filter(field -> !allBibtexFields.contains(field))
                                               .map(BibField::getField)
                                               // these fields are displayed by JabRef as default
                                               .filter(field -> !field.equals(StandardField.ABSTRACT))
                                               .filter(field -> !field.equals(StandardField.COMMENT))
                                               .filter(field -> !field.equals(StandardField.DOI))
                                               .filter(field -> !field.equals(StandardField.URL))
                                               .collect(Collectors.toSet());
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        final Set<Field> allBiblatexOnlyFields = getAllBiblatexOnlyFields();
        return entry.getFields().stream()
                    .filter(allBiblatexOnlyFields::contains)
                    .map(name -> new IntegrityMessage(Localization.lang("biblatex field only"), entry, name)).collect(Collectors.toList());
    }
}
