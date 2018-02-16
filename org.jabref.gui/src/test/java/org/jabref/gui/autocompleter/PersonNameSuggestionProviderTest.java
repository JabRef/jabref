package org.jabref.gui.autocompleter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jabref.gui.autocompleter.AutoCompleterUtil.getRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PersonNameSuggestionProviderTest {

    private static final Author vassilisKostakos = new Author("Vassilis", "V.", "", "Kostakos", "");
    private PersonNameSuggestionProvider autoCompleter;
    private BibEntry entry;

    public void initAutoCompleterWithNullFieldThrowsException() {
        assertThrows(NullPointerException.class, () -> new PersonNameSuggestionProvider((String) null));
    }

    @BeforeEach
    public void setUp() throws Exception {
        autoCompleter = new PersonNameSuggestionProvider("field");

        entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
    }

    @Test
    public void completeWithoutAddingAnythingReturnsNothing() {
        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingNullReturnsNothing() {
        autoCompleter.indexEntry(null);

        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEmptyEntryReturnsNothing() {
        BibEntry entry = new BibEntry();
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeAfterAddingEntryWithoutFieldReturnsNothing() {
        BibEntry entry = new BibEntry();
        entry.setField("title", "testTitle");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("test")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeNameReturnsName() {
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kostakos")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    public void completeBeginningOfNameReturnsName() {
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kosta")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    public void completeLowercaseBeginningOfNameReturnsName() {
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("kosta")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    public void completeNullThrowsException() {
        assertThrows(NullPointerException.class, () -> autoCompleter.call(getRequest((null))));
    }

    @Test
    public void completeEmptyStringReturnsNothing() {
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("")));
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void completeReturnsMultipleResults() {
        autoCompleter.indexEntry(entry);
        BibEntry entryTwo = new BibEntry();
        entryTwo.setField("field", "Kosta");
        autoCompleter.indexEntry(entryTwo);
        Author authorTwo = new Author("", "", "", "Kosta", "");

        Collection<Author> result = autoCompleter.call(getRequest(("Ko")));
        assertEquals(Arrays.asList(authorTwo, vassilisKostakos), result);
    }

    @Test
    public void completePartOfNameReturnsName() {
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("osta")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsName() {
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Vas")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsNameWithJr() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Reagle, Jr., Joseph M.");
        autoCompleter.indexEntry(entry);
        Author author = new Author("Joseph M.", "J. M.", "", "Reagle", "Jr.");

        Collection<Author> result = autoCompleter.call(getRequest(("Jos")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    public void completeBeginningOfFirstNameReturnsNameWithVon() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.indexEntry(entry);
        Author author = new Author("Eric", "E.", "von", "Hippel", "");

        Collection<Author> result = autoCompleter.call(getRequest(("Eric")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    public void completeBeginningOfLastNameReturnsNameWithUmlauts() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Honig Bär");
        autoCompleter.indexEntry(entry);
        Author author = new Author("Honig", "H.", "", "Bär", "");

        Collection<Author> result = autoCompleter.call(getRequest(("Bä")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    public void completeVonReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Eric von Hippel");
        autoCompleter.indexEntry(entry);
        Author author = new Author("Eric", "E.", "von", "Hippel", "");

        Collection<Author> result = autoCompleter.call(getRequest(("von")));
        assertEquals(Collections.singletonList(author), result);
    }

    @Test
    public void completeBeginningOfFullNameReturnsName() {
        BibEntry entry = new BibEntry();
        entry.setField("field", "Vassilis Kostakos");
        autoCompleter.indexEntry(entry);

        Collection<Author> result = autoCompleter.call(getRequest(("Kostakos, Va")));
        assertEquals(Collections.singletonList(vassilisKostakos), result);
    }
}
