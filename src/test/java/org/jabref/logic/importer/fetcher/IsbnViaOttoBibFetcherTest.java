package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
public class IsbnViaOttoBibFetcherTest extends AbstractIsbnFetcherTest {

    @BeforeEach
    public void setUp() {
        bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9782819502746");
        bibEntry.setField("title", "Les mots du passé : roman");
        bibEntry.setField("publisher", "́Éd. les Nouveaux auteurs");
        bibEntry.setField("year", "2012");
        bibEntry.setField("author", "Denis");
        bibEntry.setField("isbn", "978-2-8195-02746");
        bibEntry.setField("url", "https://www.ottobib.com/isbn/9782819502746/bibtex");

        fetcher = new IsbnViaOttoBibFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    @Override
    public void testName() {
        assertEquals("ISBN (OttoBib)", fetcher.getName());
    }

    @Test
    @Override
    public void testHelpPage() {
        assertEquals("ISBNtoBibTeX", fetcher.getHelpPage().get().getPageName());
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithShortISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0321356683");
        bibEntry.setField("bibtexkey", "0321356683");
        bibEntry.setField("isbn", "0321356683");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void searchByIdSuccessfulWithLongISBN() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9780321356680");
        bibEntry.setField("bibtexkey", "9780321356680");
        bibEntry.setField("isbn", "9780321356680");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    @Override
    public void authorsAreCorrectlyFormatted() throws Exception {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType(BiblatexEntryTypes.BOOK);
        bibEntry.setField("bibtexkey", "9782819502746");
        bibEntry.setField("title", "Les mots du passé : roman");
        bibEntry.setField("publisher", "́Éd. les Nouveaux auteurs");
        bibEntry.setField("year", "2012");
        bibEntry.setField("author", "Denis");
        bibEntry.setField("isbn", "978-2-8195-02746");
        bibEntry.setField("url", "https://www.ottobib.com/isbn/9782819502746/bibtex");

        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("9782819502746");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }
}
