package net.sf.jabref.logic.integrity;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryConverter;
import net.sf.jabref.model.entry.FieldName;

public class NoBibtexFieldChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        SortedSet<String> allBibLaTeXOnlyFields = new TreeSet<>();
        allBibLaTeXOnlyFields.addAll(EntryConverter.FIELD_ALIASES_LTX_TO_TEX.keySet());

        // file is both in bibtex and biblatex
        allBibLaTeXOnlyFields.remove(FieldName.FILE);

        // this exists in BibLaTeX only (and is no aliassed field)
        allBibLaTeXOnlyFields.add(FieldName.JOURNALSUBTITLE);

        return entry.getFieldNames().stream()
                .filter(name ->  allBibLaTeXOnlyFields.contains(name))
                .map(name -> new IntegrityMessage(Localization.lang("BibLaTeX field only"), entry, name)).collect(Collectors.toList());
    }

}
