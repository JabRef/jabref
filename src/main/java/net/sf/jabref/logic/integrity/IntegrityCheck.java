package net.sf.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

public class IntegrityCheck {

    private final BibDatabaseContext bibDatabaseContext;
    private final FileDirectoryPreferences fileDirectoryPreferences;

    public IntegrityCheck(BibDatabaseContext bibDatabaseContext, FileDirectoryPreferences fileDirectoryPreferences) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.fileDirectoryPreferences = Objects.requireNonNull(fileDirectoryPreferences);
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

        result.addAll(new AuthorNameChecker().check(entry));

        // BibTeX only checkers
        if (!bibDatabaseContext.isBiblatexMode()) {
            result.addAll(new TitleChecker().check(entry));
            result.addAll(new PagesChecker().check(entry));
            result.addAll(new ASCIICharacterChecker().check(entry));
            result.addAll(new NoBibtexFieldChecker().check(entry));
        } else {
            result.addAll(new BiblatexPagesChecker().check(entry));
        }

        result.addAll(new BracketChecker(FieldName.TITLE).check(entry));
        result.addAll(new YearChecker().check(entry));
        result.addAll(new BibtexkeyChecker().check(entry));
        result.addAll(new EditionChecker(bibDatabaseContext).check(entry));
        result.addAll(new NoteChecker(bibDatabaseContext).check(entry));
        result.addAll(new HowpublishedChecker(bibDatabaseContext).check(entry));
        result.addAll(new MonthChecker(bibDatabaseContext).check(entry));
        result.addAll(new UrlChecker().check(entry));
        result.addAll(new FileChecker(bibDatabaseContext, fileDirectoryPreferences).check(entry));
        result.addAll(new TypeChecker().check(entry));
        for (String journalField : InternalBibtexFields.getJournalNameFields()) {
            result.addAll(new AbbreviationChecker(journalField).check(entry));
        }
        for (String bookNameField : InternalBibtexFields.getBookNameFields()) {
            result.addAll(new AbbreviationChecker(bookNameField).check(entry));
        }
        result.addAll(new BibStringChecker().check(entry));
        result.addAll(new HTMLCharacterChecker().check(entry));
        result.addAll(new BooktitleChecker().check(entry));
        result.addAll(new ISSNChecker().check(entry));
        result.addAll(new ISBNChecker().check(entry));
        result.addAll(new EntryLinkChecker(bibDatabaseContext.getDatabase()).check(entry));

        return result;
    }


    @FunctionalInterface
    public interface Checker {
        List<IntegrityMessage> check(BibEntry entry);
    }
}
