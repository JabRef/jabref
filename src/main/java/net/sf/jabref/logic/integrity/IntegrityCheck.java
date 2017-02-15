package net.sf.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

public class IntegrityCheck {

    private final BibDatabaseContext bibDatabaseContext;
    private final FileDirectoryPreferences fileDirectoryPreferences;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;

    public IntegrityCheck(BibDatabaseContext bibDatabaseContext,
            FileDirectoryPreferences fileDirectoryPreferences,
            BibtexKeyPatternPreferences bibtexKeyPatternPreferences
    ) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.fileDirectoryPreferences = Objects.requireNonNull(fileDirectoryPreferences);
        this.bibtexKeyPatternPreferences = Objects.requireNonNull(bibtexKeyPatternPreferences);
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

        for (FieldChecker checker : FieldCheckers.getAll(bibDatabaseContext, fileDirectoryPreferences)) {
            result.addAll(checker.check(entry));
        }

        if (!bibDatabaseContext.isBiblatexMode()) {
            // BibTeX only checkers
            result.addAll(new ASCIICharacterChecker().check(entry));
            result.addAll(new NoBibtexFieldChecker().check(entry));
            result.addAll(new BibTeXEntryTypeChecker().check(entry));
        }

        result.addAll(new BibtexkeyChecker().check(entry));
        result.addAll(new TypeChecker().check(entry));
        result.addAll(new BibStringChecker().check(entry));
        result.addAll(new HTMLCharacterChecker().check(entry));
        result.addAll(new EntryLinkChecker(bibDatabaseContext.getDatabase()).check(entry));
        result.addAll(new BibtexkeyDeviationChecker(bibDatabaseContext, bibtexKeyPatternPreferences).check(entry));

        return result;
    }


    @FunctionalInterface
    public interface Checker {
        List<IntegrityMessage> check(BibEntry entry);
    }
}
