package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class IntegrityCheckTest {

    @Test
    void bibTexAcceptsStandardEntryType() {
        assertCorrect(withMode(createContext(StandardField.TITLE, "sometitle", StandardEntryType.Article), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptIEEETranEntryType() {
        assertWrong(withMode(createContext(StandardField.TITLE, "sometitle", IEEETranEntryType.Patent), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsIEEETranEntryType() {
        assertCorrect((withMode(createContext(StandardField.TITLE, "sometitle", IEEETranEntryType.Patent), BibDatabaseMode.BIBLATEX)));
    }

    @Test
    void bibLaTexAcceptsStandardEntryType() {
        assertCorrect(withMode(createContext(StandardField.TITLE, "sometitle", StandardEntryType.Article), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void testEntryIsUnchangedAfterChecks() {
        BibEntry entry = new BibEntry();

        // populate with all known fields
        for (Field field : FieldFactory.getCommonFields()) {
            entry.setField(field, UUID.randomUUID().toString());
        }
        // add a random field
        entry.setField(StandardField.EPRINT, UUID.randomUUID().toString());

        // duplicate entry
        BibEntry clonedEntry = (BibEntry) entry.clone();

        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);

        new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                           new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true, false)
                .checkDatabase();

        assertEquals(clonedEntry, entry);
    }

    protected static BibDatabaseContext createContext(Field field, String value, EntryType type) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        entry.setType(type);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase);
    }

    protected static BibDatabaseContext createContext(Field field, String value, MetaData metaData) {
        BibEntry entry = new BibEntry();
        entry.setField(field, value);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntry(entry);
        return new BibDatabaseContext(bibDatabase, metaData);
    }

    protected static BibDatabaseContext createContext(Field field, String value) {
        MetaData metaData = new MetaData();
        metaData.setMode(BibDatabaseMode.BIBTEX);
        return createContext(field, value, metaData);
    }

    protected static void assertWrong(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true, false)
                .checkDatabase();
        assertNotEquals(Collections.emptyList(), messages);
    }

    protected static void assertCorrect(BibDatabaseContext context) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                mock(FilePreferences.class),
                createBibtexKeyPatternPreferences(),
                new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true, false
        ).checkDatabase();
        assertEquals(Collections.emptyList(), messages);
    }

    protected static void assertCorrect(BibDatabaseContext context, boolean allowIntegerEdition) {
        List<IntegrityMessage> messages = new IntegrityCheck(context,
                                                             mock(FilePreferences.class),
                                                             createBibtexKeyPatternPreferences(),
                                                             new JournalAbbreviationRepository(new Abbreviation("IEEE Software", "IEEE SW")), true,
                                                             allowIntegerEdition).checkDatabase();
        assertEquals(Collections.emptyList(), messages);
    }

    private static BibtexKeyPatternPreferences createBibtexKeyPatternPreferences() {
        final GlobalBibtexKeyPattern keyPattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");
        return new BibtexKeyPatternPreferences(
                "",
                "",
                false,
                false,
                false,
                keyPattern,
                ',');
    }

    protected static BibDatabaseContext withMode(BibDatabaseContext context, BibDatabaseMode mode) {
        context.setMode(mode);
        return context;
    }
}