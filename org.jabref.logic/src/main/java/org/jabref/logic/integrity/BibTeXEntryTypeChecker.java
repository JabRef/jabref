package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.integrity.IntegrityCheck.Checker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.entry.BibEntry;

/**
 * BibTeX mode only checker
 */
public class BibTeXEntryTypeChecker implements Checker {
    /**
     * Will check if the current library uses any entry types from another mode.
     * For example it will warn the user if he uses entry types defined for Biblatex inside a BibTeX library.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (EntryTypes.isExclusiveBiblatex(entry.getType())) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("Entry type %0 is only defined for Biblatex but not for BibTeX", entry.getType()), entry, "bibtexkey")
            );
        }
        return Collections.emptyList();
    }
}
