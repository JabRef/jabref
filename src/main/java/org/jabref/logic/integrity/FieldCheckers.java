package org.jabref.logic.integrity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.metadata.FileDirectoryPreferences;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class FieldCheckers {

    private Multimap<String, ValueChecker> fieldChecker;

    public FieldCheckers(BibDatabaseContext databaseContext, FileDirectoryPreferences fileDirectoryPreferences, JournalAbbreviationRepository abbreviationRepository, boolean enforceLegalKey) {
        fieldChecker = getAllMap(databaseContext, fileDirectoryPreferences, abbreviationRepository, enforceLegalKey);
    }

    private static Multimap<String, ValueChecker> getAllMap(BibDatabaseContext databaseContext, FileDirectoryPreferences fileDirectoryPreferences, JournalAbbreviationRepository abbreviationRepository, boolean enforceLegalKey) {
        ArrayListMultimap<String, ValueChecker> fieldCheckers = ArrayListMultimap.create(50, 10);

        for (String field : InternalBibtexFields.getJournalNameFields()) {
            fieldCheckers.put(field, new AbbreviationChecker(abbreviationRepository));
        }
        for (String field : InternalBibtexFields.getBookNameFields()) {
            fieldCheckers.put(field, new AbbreviationChecker(abbreviationRepository));
        }
        for (String field : InternalBibtexFields.getPersonNameFields()) {
            fieldCheckers.put(field, new PersonNamesChecker(databaseContext));
        }
        fieldCheckers.put(FieldName.BOOKTITLE, new BooktitleChecker());
        fieldCheckers.put(FieldName.TITLE, new BracketChecker());
        fieldCheckers.put(FieldName.TITLE, new TitleChecker(databaseContext));
        fieldCheckers.put(FieldName.DOI, new DOIValidityChecker());
        fieldCheckers.put(FieldName.EDITION, new EditionChecker(databaseContext));
        fieldCheckers.put(FieldName.FILE, new FileChecker(databaseContext, fileDirectoryPreferences));
        fieldCheckers.put(FieldName.HOWPUBLISHED, new HowPublishedChecker(databaseContext));
        fieldCheckers.put(FieldName.ISBN, new ISBNChecker());
        fieldCheckers.put(FieldName.ISSN, new ISSNChecker());
        fieldCheckers.put(FieldName.MONTH, new MonthChecker(databaseContext));
        fieldCheckers.put(FieldName.MONTHFILED, new MonthChecker(databaseContext));
        fieldCheckers.put(FieldName.NOTE, new NoteChecker(databaseContext));
        fieldCheckers.put(FieldName.PAGES, new PagesChecker(databaseContext));
        fieldCheckers.put(FieldName.URL, new UrlChecker());
        fieldCheckers.put(FieldName.YEAR, new YearChecker());
        fieldCheckers.put(FieldName.KEY, new ValidBibtexKeyChecker(enforceLegalKey));

        return fieldCheckers;
    }

    public List<FieldChecker> getAll() {
        return fieldChecker
                .entries()
                .stream()
                .map(pair -> new FieldChecker(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<ValueChecker> getForField(String field) {
        return fieldChecker
                .get(field);
    }
}
