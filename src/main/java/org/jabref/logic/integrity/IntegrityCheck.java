package org.jabref.logic.integrity;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.FilePreferences;

public class IntegrityCheck {

    private final BibDatabaseContext bibDatabaseContext;
    private final FieldCheckers fieldCheckers;
    private final List<EntryChecker> entryCheckers;

    public IntegrityCheck(BibDatabaseContext bibDatabaseContext,
                          FilePreferences filePreferences,
                          CitationKeyPatternPreferences citationKeyPatternPreferences,
                          Charset defaultEncoding,
                          JournalAbbreviationRepository journalAbbreviationRepository,
                          boolean allowIntegerEdition) {
        this.bibDatabaseContext = bibDatabaseContext;

        fieldCheckers = new FieldCheckers(bibDatabaseContext,
                filePreferences,
                journalAbbreviationRepository,
                allowIntegerEdition);

        entryCheckers = new ArrayList<>(List.of(
                new CitationKeyChecker(),
                new TypeChecker(),
                new BibStringChecker(),
                new HTMLCharacterChecker(),
                new EntryLinkChecker(bibDatabaseContext.getDatabase()),
                new CitationKeyDeviationChecker(bibDatabaseContext, citationKeyPatternPreferences),
                new CitationKeyDuplicationChecker(bibDatabaseContext.getDatabase())
        ));
        if (bibDatabaseContext.isBiblatexMode()) {
            entryCheckers.addAll(List.of(
                    new JournalInAbbreviationListChecker(StandardField.JOURNALTITLE, journalAbbreviationRepository),
                    new UTF8Checker(bibDatabaseContext.getMetaData().getEncoding().orElse(defaultEncoding))
            ));
        } else {
            entryCheckers.addAll(List.of(
                    new JournalInAbbreviationListChecker(StandardField.JOURNAL, journalAbbreviationRepository),
                    new ASCIICharacterChecker(),
                    new NoBibtexFieldChecker(),
                    new BibTeXEntryTypeChecker())
            );
        }
    }

    List<IntegrityMessage> check() {
        List<IntegrityMessage> result = new ArrayList<>();

        BibDatabase database = bibDatabaseContext.getDatabase();

        for (BibEntry entry : database.getEntries()) {
            result.addAll(checkEntry(entry));
        }
        result.addAll(checkDatabase(database));

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

        for (EntryChecker entryChecker : entryCheckers) {
            result.addAll(entryChecker.check(entry));
        }

        return result;
    }

    public List<IntegrityMessage> checkDatabase(BibDatabase database) {
        return new DoiDuplicationChecker().check(database);
    }
}
