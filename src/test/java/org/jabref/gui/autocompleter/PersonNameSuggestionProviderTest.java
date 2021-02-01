package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersonNameSuggestionProviderTest {

    private final Author vassilisKostakos = new Author("Vassilis", "V.", "", "Kostakos", "");
    private PersonNameSuggestionProvider autoCompleter;
    private BibEntry entry;
    private BibDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        database = new BibDatabase();
        autoCompleter = new PersonNameSuggestionProvider(StandardField.AUTHOR, database);

        entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Vassilis Kostakos");
    }

    @Test
    void initAutoCompleterWithNullFieldThrowsException() {
        assertThrows(NullPointerException.class, () -> new PersonNameSuggestionProvider((Field) null, new BibDatabase()));
    }

    @Test
    void completeWithoutAddingAnythingReturnsNothing() {
        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "testTitle");
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeNameReturnsName() {
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Kostakos")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    void completeBeginningOfNameReturnsName() {
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Kosta")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    void completeLowercaseBeginningOfNameReturnsName() {
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("kosta")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    void completeNullThrowsException() {
        assertThrows(NullPointerException.class, () -> autoCompleter.provideSuggestions(getRequest((null))));
    }

    @Test
    void completeEmptyStringReturnsNothing() {
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void completeReturnsMultipleResults() {
        database.insertEntry(entry);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField(StandardField.AUTHOR, "Kosta");
        database.insertEntry(entryTwo);
        Author authorTwo = new Author("", "", "", "Kosta", "");

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Ko")));
        assertEquals(Arrays.asList(authorTwo, vassilisKostakos), result);
    }

    @Test
    void completePartOfNameReturnsName() {
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("osta")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    void completeBeginningOfFirstNameReturnsName() {
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Vas")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    void completeBeginningOfFirstNameReturnsNameWithJr() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Reagle, Jr., Joseph M.");
        database.insertEntry(entry);
        Author author = new Author("Joseph M.", "J. M.", "", "Reagle", "Jr.");

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Jos")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    void completeBeginningOfFirstNameReturnsNameWithVon() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Eric von Hippel");
        database.insertEntry(entry);
        Author author = new Author("Eric", "E.", "von", "Hippel", "");

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Eric")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    void completeBeginningOfLastNameReturnsNameWithUmlauts() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Honig Bär");
        database.insertEntry(entry);
        Author author = new Author("Honig", "H.", "", "Bär", "");

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Bä")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    void completeVonReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Eric von Hippel");
        database.insertEntry(entry);
        Author author = new Author("Eric", "E.", "von", "Hippel", "");

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("von")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    void completeBeginningOfFullNameReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Vassilis Kostakos");
        database.insertEntry(entry);

        Collection<Author> result = autoCompleter.provideSuggestions(getRequest(("Kostakos, Va")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }
}
