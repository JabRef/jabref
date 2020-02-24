package org.jabref.logic.importer.fetcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.testutils.category.FetcherTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

@FetcherTest
public class WorldcatFetcherTest{

	private WorldcatFetcher fetcher;

	@BeforeEach
	public void setUp() {
		fetcher = new WorldcatFetcher();
	}

	@Test
	public void testPerformSearchForBadTitle() throws FetcherException{
		BibEntry entry = new BibEntry();
		//Mashing keyboard. Verified on https://platform.worldcat.org/api-explorer/apis/wcapi/Bib/OpenSearch
		entry.setField(StandardField.TITLE, "ASDhbsd fnm");
		List<BibEntry> list = fetcher.performSearch(entry);
		assertTrue(list.isEmpty());
	}

	@Test
	public void testPerformSearchForExistingTitle() throws FetcherException{
		BibEntry entry = new BibEntry();
		//Example "The very best of Glenn Miller". Verified on same link as above
		entry.setField(StandardField.TITLE, "The very best of Glenn");
		List<BibEntry> list = fetcher.performSearch(entry);
		assertFalse(list.isEmpty());
	}

	@Test
	public void testPerformSearchForNullTitle() throws FetcherException{
		BibEntry entry = new BibEntry();
		entry.setField(StandardField.TITLE, null);
		List<BibEntry> list = fetcher.performSearch(entry);
		assertTrue(list.isEmpty());
	}

}