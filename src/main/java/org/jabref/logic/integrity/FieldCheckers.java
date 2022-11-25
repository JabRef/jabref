package org.jabref.logic.integrity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class FieldCheckers {

    private final Multimap<Field, ValueChecker> fieldChecker;

    public FieldCheckers(BibDatabaseContext databaseContext, FilePreferences filePreferences,
                         JournalAbbreviationRepository abbreviationRepository, boolean allowIntegerEdition) {
        fieldChecker = getAllMap(databaseContext, filePreferences, abbreviationRepository, allowIntegerEdition);
    }

    private static Multimap<Field, ValueChecker> getAllMap(BibDatabaseContext databaseContext, FilePreferences filePreferences, JournalAbbreviationRepository abbreviationRepository, boolean allowIntegerEdition) {
        ArrayListMultimap<Field, ValueChecker> fieldCheckers = ArrayListMultimap.create(50, 10);

        for (Field field : FieldFactory.getBookNameFields()) {
            fieldCheckers.put(field, new AbbreviationChecker(abbreviationRepository));
        }
        for (Field field : FieldFactory.getPersonNameFields()) {
            fieldCheckers.put(field, new PersonNamesChecker(databaseContext));
        }
        fieldCheckers.put(StandardField.BOOKTITLE, new BooktitleChecker());
        fieldCheckers.put(StandardField.TITLE, new BracketChecker());
        fieldCheckers.put(StandardField.TITLE, new TitleChecker(databaseContext));
        fieldCheckers.put(StandardField.DOI, new DoiValidityChecker());
        fieldCheckers.put(StandardField.EDITION, new EditionChecker(databaseContext, allowIntegerEdition));
        fieldCheckers.put(StandardField.FILE, new FileChecker(databaseContext, filePreferences));
        fieldCheckers.put(StandardField.HOWPUBLISHED, new HowPublishedChecker(databaseContext));
        fieldCheckers.put(StandardField.ISBN, new ISBNChecker());
        fieldCheckers.put(StandardField.ISSN, new ISSNChecker());
        fieldCheckers.put(StandardField.MONTH, new MonthChecker(databaseContext));
        fieldCheckers.put(StandardField.MONTHFILED, new MonthChecker(databaseContext));
        fieldCheckers.put(StandardField.NOTE, new NoteChecker(databaseContext));
        fieldCheckers.put(StandardField.PAGES, new PagesChecker(databaseContext));
        fieldCheckers.put(StandardField.URL, new UrlChecker());
        fieldCheckers.put(StandardField.YEAR, new YearChecker());
        fieldCheckers.put(StandardField.KEY, new ValidCitationKeyChecker());
        fieldCheckers.put(InternalField.KEY_FIELD, new ValidCitationKeyChecker());

        if (databaseContext.isBiblatexMode()) {
            fieldCheckers.put(StandardField.DATE, new DateChecker());
            fieldCheckers.put(StandardField.URLDATE, new DateChecker());
            fieldCheckers.put(StandardField.EVENTDATE, new DateChecker());
            fieldCheckers.put(StandardField.ORIGDATE, new DateChecker());
        }

        return fieldCheckers;
    }

    public List<FieldChecker> getAll() {
        return fieldChecker
                .entries()
                .stream()
                .map(pair -> new FieldChecker(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());
    }

    public Collection<ValueChecker> getForField(Field field) {
        return fieldChecker
                .get(field);
    }
}
