package org.jabref.logic.citationstyle;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabRefItemDataProviderTest {

    @Test
    void toJsonOneEntry() {
        BibDatabase bibDatabase = new BibDatabase(List.of(
                new BibEntry()
                        .withCitationKey("key")
                        .withField(StandardField.AUTHOR, "Test Author")
        ));
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(bibDatabase);
        JabRefItemDataProvider jabRefItemDataProvider = new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(bibDatabaseContext, new BibEntryTypesManager());
        assertEquals("""
                        [{"id":"key","type":"article","author":[{"family":"Author","given":"Test"}]}]""",
                jabRefItemDataProvider.toJson());
    }

    @Test
    void toJsonTwoEntries() {
        BibDatabase bibDatabase = new BibDatabase(List.of(
                new BibEntry()
                        .withCitationKey("key")
                        .withField(StandardField.AUTHOR, "Test Author"),
                new BibEntry()
                        .withCitationKey("key2")
                        .withField(StandardField.AUTHOR, "Second Author")
        ));
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(bibDatabase);
        JabRefItemDataProvider jabRefItemDataProvider = new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(bibDatabaseContext, new BibEntryTypesManager());
        assertEquals("""
                        [{"id":"key","type":"article","author":[{"family":"Author","given":"Test"}]},{"id":"key2","type":"article","author":[{"family":"Author","given":"Second"}]}]""",
                jabRefItemDataProvider.toJson());
    }
}
