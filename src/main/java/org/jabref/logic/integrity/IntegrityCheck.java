package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.FilePreferences;

public class IntegrityCheck {

    private final BibDatabaseContext bibDatabaseContext;
    private final FieldCheckers fieldCheckers;
    private final List<Checker> entryCheckers;

    public IntegrityCheck(BibDatabaseContext bibDatabaseContext,
                          FilePreferences filePreferences,
                          BibtexKeyPatternPreferences bibtexKeyPatternPreferences,
                          JournalAbbreviationRepository journalAbbreviationRepository,
                          boolean enforceLegalKey,
                          boolean allowIntegerEdition) {
        this.bibDatabaseContext = bibDatabaseContext;

        fieldCheckers = new FieldCheckers(bibDatabaseContext,
                filePreferences,
                journalAbbreviationRepository,
                enforceLegalKey,
                allowIntegerEdition);

        entryCheckers = Arrays.asList(
                new BibtexKeyChecker(),
                new TypeChecker(),
                new BibStringChecker(),
                new HTMLCharacterChecker(),
                new EntryLinkChecker(bibDatabaseContext.getDatabase()),
                new BibtexkeyDeviationChecker(bibDatabaseContext, bibtexKeyPatternPreferences),
                new BibtexKeyDuplicationChecker(bibDatabaseContext.getDatabase()),
                new DoiDuplicationChecker(bibDatabaseContext.getDatabase())
        );

        if (!bibDatabaseContext.isBiblatexMode()) {
            entryCheckers.add(new JournalInAbbreviationListChecker(StandardField.JOURNALTITLE, journalAbbreviationRepository));
        } else {
            entryCheckers.addAll(List.of(
                    new JournalInAbbreviationListChecker(StandardField.JOURNAL, journalAbbreviationRepository),
                    new ASCIICharacterChecker(),
                    new NoBibtexFieldChecker(),
                    new BibTeXEntryTypeChecker())
            );
        }
    }

    List<IntegrityMessage> checkDatabase() {
        List<IntegrityMessage> result = new ArrayList<>();

        for (BibEntry entry : bibDatabaseContext.getDatabase().getEntries()) {
            result.addAll(checkEntry(entry));
        }

        return result;
    }

    public List<IntegrityMessage> checkEntry(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();
        if (entry == null) {
            return result;
        }

        for (FieldChecker fieldChecker : fieldCheckers.getAll()) {
            result.addAll(fieldChecker.check(entry));
        }

        for (Checker entryChecker : entryCheckers) {
            result.addAll(entryChecker.check(entry));
        }

        return result;
    }
}
