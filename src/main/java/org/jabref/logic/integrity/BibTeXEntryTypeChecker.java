package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.types.EntryTypeFactory;

/**
 * BibTeX mode only checker
 */
public class BibTeXEntryTypeChecker implements EntryChecker {
    /**
     * Will check if the current library uses any entry types from another mode.
     * For example it will warn the user if he uses entry types defined for Biblatex inside a BibTeX library.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (EntryTypeFactory.isExclusiveBiblatex(entry.getType())) {
            return Collections.singletonList(
                    new IntegrityMessage(IntegrityIssue.ENTRY_TYPE_IS_ONLY_DEFINED_FOR_BIBLATEX_BUT_NOT_FOR_BIBTEX, entry, InternalField.KEY_FIELD)
            );
        }
        return Collections.emptyList();
    }
}
