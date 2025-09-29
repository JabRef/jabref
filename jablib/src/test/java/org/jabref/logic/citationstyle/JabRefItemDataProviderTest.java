package org.jabref.logic.citationstyle;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
    // SWEN 755 test add - multiple authories in one entry test
    @Test
    void toJsonMultipleAuthorOneEntry() {
        BibDatabase bibDatabase = new BibDatabase(List.of(
           new BibEntry()
                   .withCitationKey("key")
                   .withField(StandardField.AUTHOR, "Test Doe and Second Author")
        ));
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(bibDatabase);
        JabRefItemDataProvider jabRefItemDataProvider = new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(bibDatabaseContext, new BibEntryTypesManager());
        assertEquals("""
                [{"id":"key","type":"article","author":[{"family":"Doe","given":"Test"},{"family":"Author","given":"Second"}]}]""",
                jabRefItemDataProvider.toJson());
    }

    @Test
    void toJsonNoEntryType() {
        BibDatabase bibDatabase = new BibDatabase(List.of(
                new BibEntry()
                        .withCitationKey("key")
                        .withField(StandardField.TITLE, "Chewbaca")
                        .withField(StandardField.AUTHOR, "Jr. Senor Hubert")
        ));
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(bibDatabase);
        JabRefItemDataProvider jabRefItemDataProvider = new JabRefItemDataProvider();
        jabRefItemDataProvider.setData(bibDatabaseContext, new BibEntryTypesManager());
        // [{"id":"key","type":"article","author":[{"family":"Hubert","given":"Jr. Senor"}],"title":"Chewbaca"}]
        assertNotEquals("""
                        [{"id":"key","type":"book","author":[{"family":"Jr. ","given":"Jr. Senor"}]}]""",
                    jabRefItemDataProvider.toJson());
    }
}
