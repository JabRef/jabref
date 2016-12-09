package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.EntryType;

/**
 * BibTeX mode only checker
 */
public class BibTeXEntryTypeChecker implements Checker {
    private static final List<EntryType> BIBTEX = BibtexEntryTypes.ALL;
    private static final List<EntryType> BIBLATEX = BibLatexEntryTypes.ALL;
    private static final List<String> EXCLUSIVE_BIBLATEX = filterEntryTypesNames(BIBLATEX, isNotIncludedIn(BIBTEX));

    /**
     * Will check if the current library uses any entry types from another mode.
     * For example it will warn the user if he uses entry types defined for Biblatex inside a BibTeX library.
     */
    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (EXCLUSIVE_BIBLATEX.contains(entry.getType())) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("Entry type %0 is only defined for Biblatex but not for BibTeX", entry.getType()), entry, "bibtexkey")
            );
        }
        return Collections.emptyList();
    }

    private static List<String> filterEntryTypesNames(List<EntryType> types, Predicate<EntryType> predicate) {
        return types.stream().filter(predicate).map(type -> type.getName().toLowerCase()).collect(Collectors.toList());
    }

    private static Predicate<EntryType> isNotIncludedIn(List<EntryType> collection) {
        return entry -> collection.stream().noneMatch(c -> c.getName().equalsIgnoreCase(entry.getName()));
    }
}
