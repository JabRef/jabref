package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.l10n.Localization;
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
                    new IntegrityMessage(Localization.lang("Entry type %0 is only defined for Biblatex but not for BibTeX", entry.getType().getDisplayName()), entry, InternalField.KEY_FIELD)
            );
        }
        return Collections.emptyList();
    }
}
