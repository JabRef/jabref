package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.FileDirectoryPreferences;

public class IntegrityCheck {

    private final BibDatabaseContext bibDatabaseContext;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private final JournalAbbreviationRepository journalAbbreviationRepository;
    private final boolean enforceLegalKey;

    public IntegrityCheck(BibDatabaseContext bibDatabaseContext,
                          FileDirectoryPreferences fileDirectoryPreferences,
                          BibtexKeyPatternPreferences bibtexKeyPatternPreferences,
                          JournalAbbreviationRepository journalAbbreviationRepository,
                          boolean enforceLegalKey) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.fileDirectoryPreferences = Objects.requireNonNull(fileDirectoryPreferences);
        this.bibtexKeyPatternPreferences = Objects.requireNonNull(bibtexKeyPatternPreferences);
        this.journalAbbreviationRepository = Objects.requireNonNull(journalAbbreviationRepository);
        this.enforceLegalKey = enforceLegalKey;
    }

    public List<IntegrityMessage> checkBibtexDatabase() {
        List<IntegrityMessage> result = new ArrayList<>();

        for (BibEntry entry : bibDatabaseContext.getDatabase().getEntries()) {
            result.addAll(checkBibtexEntry(entry));
        }

        return result;
    }

    private List<IntegrityMessage> checkBibtexEntry(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();

        if (entry == null) {
            return result;
        }

        FieldCheckers fieldCheckers = new FieldCheckers(bibDatabaseContext, fileDirectoryPreferences, journalAbbreviationRepository, enforceLegalKey);
        for (FieldChecker checker : fieldCheckers.getAll()) {
            result.addAll(checker.check(entry));
        }

        if (!bibDatabaseContext.isBiblatexMode()) {
            // BibTeX only checkers
            result.addAll(new ASCIICharacterChecker().check(entry));
            result.addAll(new NoBibtexFieldChecker().check(entry));
            result.addAll(new BibTeXEntryTypeChecker().check(entry));
            result.addAll(new JournalInAbbreviationListChecker(FieldName.JOURNAL, journalAbbreviationRepository).check(entry));
        } else {
            result.addAll(new JournalInAbbreviationListChecker(FieldName.JOURNALTITLE, journalAbbreviationRepository).check(entry));
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
