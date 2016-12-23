package net.sf.jabref.logic.integrity;

import java.util.List;
import java.util.stream.Collectors;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;

/**
 * Checks for non BibTeX or BibLaTeX fields
 */
public class NoBibtexOrBiblatexFieldChecker implements Checker {


    private List<String> getAllBiblatexFields() {
        return BibLatexEntryTypes.ALL.stream().flatMap(type -> type.getAllFields().stream())
                .collect(Collectors.toList());
    }

    private List<String> getAllBibtexFields() {
        return BibtexEntryTypes.ALL.stream().flatMap(type -> type.getAllFields().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        final List<String> allBiblatexFields = getAllBiblatexFields();
        allBiblatexFields.addAll(getAllBibtexFields());
        // Add internal fields
        allBiblatexFields.add(FieldName.TIMESTAMP);
        allBiblatexFields.add(FieldName.OWNER);
        allBiblatexFields.add(FieldName.ABSTRACT);
        allBiblatexFields.add(FieldName.REVIEW);
        allBiblatexFields.add(FieldName.COMMENT);
        allBiblatexFields.add(FieldName.KEYWORDS);
        allBiblatexFields.add(FieldName.FILE);
        allBiblatexFields.add(BibEntry.KEY_FIELD);
        // Add some missing BIBLATEX_PERSON_NAME_FIELDS
        allBiblatexFields.add(FieldName.SHORTAUTHOR);
        allBiblatexFields.add(FieldName.SHORTEDITOR);
        allBiblatexFields.add(FieldName.SORTNAME);
        allBiblatexFields.add(FieldName.NAMEADDON);
        allBiblatexFields.add(FieldName.ASSIGNEE);
        return entry.getFieldNames().stream().filter(name -> !allBiblatexFields.contains(name))
                .map(name -> new IntegrityMessage(Localization.lang("No BibTeX/BibLaTeX field"), entry, name))
                .collect(Collectors.toList());
    }

}
