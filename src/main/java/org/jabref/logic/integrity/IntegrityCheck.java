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
    private ASCIICharacterChecker asciiCharacterChecker;
    private NoBibtexFieldChecker noBibtexFieldChecker;
    private BibTeXEntryTypeChecker bibTeXEntryTypeChecker;
    private BibtexKeyChecker bibtexKeyChecker;
    private TypeChecker typeChecker;
    private BibStringChecker bibStringChecker;
    private HTMLCharacterChecker htmlCharacterChecker;
    private EntryLinkChecker entryLinkChecker;
    private BibtexkeyDeviationChecker bibtexkeyDeviationChecker;
    private BibtexKeyDuplicationChecker bibtexKeyDuplicationChecker;
    private JournalInAbbreviationListChecker journalInAbbreviationListChecker;
    private FieldCheckers fieldCheckers;
    private DoiDuplicationChecker doiDuplicationChecker;

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
        initCheckers(bibDatabaseContext, bibtexKeyPatternPreferences, journalAbbreviationRepository);
    }

    private void initCheckers(BibDatabaseContext bibDatabaseContext, BibtexKeyPatternPreferences bibtexKeyPatternPreferences, JournalAbbreviationRepository journalAbbreviationRepository) {
        asciiCharacterChecker = new ASCIICharacterChecker();
        noBibtexFieldChecker = new NoBibtexFieldChecker();
        bibTeXEntryTypeChecker = new BibTeXEntryTypeChecker();
        bibtexKeyChecker = new BibtexKeyChecker();
        typeChecker = new TypeChecker();
        bibStringChecker = new BibStringChecker();
        htmlCharacterChecker = new HTMLCharacterChecker();
        entryLinkChecker = new EntryLinkChecker(bibDatabaseContext.getDatabase());
        bibtexkeyDeviationChecker = new BibtexkeyDeviationChecker(bibDatabaseContext, bibtexKeyPatternPreferences);
        bibtexKeyDuplicationChecker = new BibtexKeyDuplicationChecker(bibDatabaseContext.getDatabase());
        doiDuplicationChecker = new DoiDuplicationChecker(bibDatabaseContext.getDatabase());

        if (bibDatabaseContext.isBiblatexMode()) {
            journalInAbbreviationListChecker = new JournalInAbbreviationListChecker(StandardField.JOURNALTITLE, journalAbbreviationRepository);
        } else {
            journalInAbbreviationListChecker = new JournalInAbbreviationListChecker(StandardField.JOURNAL, journalAbbreviationRepository);
        }

        fieldCheckers = new FieldCheckers(bibDatabaseContext,
                filePreferences,
                journalAbbreviationRepository,
                enforceLegalKey,
                allowIntegerEdition);
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

        for (FieldChecker checker : fieldCheckers.getAll()) {
            result.addAll(checker.check(entry));
        }

        if (!bibDatabaseContext.isBiblatexMode()) {
            // BibTeX only checkers
            result.addAll(asciiCharacterChecker.check(entry));
            result.addAll(noBibtexFieldChecker.check(entry));
            result.addAll(bibTeXEntryTypeChecker.check(entry));
        }

        result.addAll(journalInAbbreviationListChecker.check(entry));
        result.addAll(bibtexKeyChecker.check(entry));
        result.addAll(typeChecker.check(entry));
        result.addAll(bibStringChecker.check(entry));
        result.addAll(htmlCharacterChecker.check(entry));
        result.addAll(entryLinkChecker.check(entry));
        result.addAll(bibtexkeyDeviationChecker.check(entry));
        result.addAll(bibtexKeyDuplicationChecker.check(entry));
        result.addAll(doiDuplicationChecker.check(entry));

        return result;
    }
}
