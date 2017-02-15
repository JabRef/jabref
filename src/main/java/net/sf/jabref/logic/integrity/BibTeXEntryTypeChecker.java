package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.entry.BibEntry;

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
        if (EntryTypes.isExclusiveBibLatex(entry.getType())) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("Entry type %0 is only defined for Biblatex but not for BibTeX", entry.getType()), entry, "bibtexkey")
            );
        }
        return Collections.emptyList();
    }
}
