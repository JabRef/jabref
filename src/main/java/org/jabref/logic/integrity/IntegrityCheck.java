package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.FilePreferences;

public class IntegrityCheck {

    private final BibDatabaseContext bibDatabaseContext;
    private final FilePreferences filePreferences;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final boolean enforceLegalKey;
    private final boolean allowIntegerEdition;

    public IntegrityCheck(BibDatabaseContext bibDatabaseContext,
                          FilePreferences filePreferences,
                          BibtexKeyPatternPreferences bibtexKeyPatternPreferences,
                          JournalAbbreviationRepository journalAbbreviationRepository,
                          boolean enforceLegalKey,
                          boolean allowIntegerEdition) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.filePreferences = Objects.requireNonNull(filePreferences);
        this.bibtexKeyPatternPreferences = Objects.requireNonNull(bibtexKeyPatternPreferences);
        this.journalAbbreviationRepository = Objects.requireNonNull(journalAbbreviationRepository);
        this.enforceLegalKey = enforceLegalKey;
        this.allowIntegerEdition = allowIntegerEdition;
    }

    public List<IntegrityMessage> checkDatabase() {
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

        FieldCheckers fieldCheckers = new FieldCheckers(bibDatabaseContext,
                                                        filePreferences,
                                                        journalAbbreviationRepository,
                                                        enforceLegalKey,
                                                        allowIntegerEdition);
        for (FieldChecker checker : fieldCheckers.getAll()) {
            result.addAll(checker.check(entry));
        }

        if (!bibDatabaseContext.isBiblatexMode()) {
            // BibTeX only checkers
            result.addAll(new ASCIICharacterChecker().check(entry));
            result.addAll(new NoBibtexFieldChecker().check(entry));
            result.addAll(new BibTeXEntryTypeChecker().check(entry));
            result.addAll(new JournalInAbbreviationListChecker(StandardField.JOURNAL, journalAbbreviationRepository).check(entry));
        } else {
            result.addAll(new JournalInAbbreviationListChecker(StandardField.JOURNALTITLE, journalAbbreviationRepository).check(entry));
        }

        result.addAll(new BibtexKeyChecker().check(entry));
        result.addAll(new TypeChecker().check(entry));
        result.addAll(new BibStringChecker().check(entry));
        result.addAll(new HTMLCharacterChecker().check(entry));
        result.addAll(new EntryLinkChecker(bibDatabaseContext.getDatabase()).check(entry));
        result.addAll(new BibtexkeyDeviationChecker(bibDatabaseContext, bibtexKeyPatternPreferences).check(entry));
        result.addAll(new BibtexKeyDuplicationChecker(bibDatabaseContext.getDatabase()).check(entry));

        return result;
    }

    @FunctionalInterface
    public interface Checker {
        List<IntegrityMessage> check(BibEntry entry);
    }
}
