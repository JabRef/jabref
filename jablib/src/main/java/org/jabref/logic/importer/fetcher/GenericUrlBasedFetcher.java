package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.UrlBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

public class GenericUrlBasedFetcher implements UrlBasedFetcher {

    @Override
   public List<BibEntry> performSearch(String url) throws FetcherException {
        BibEntry entery = new BibEntry(StandardEntryType.Misc);
        entery.setField(StandardField.URL, url);
        return List.of(entery);
    }

    @Override
   public String getName() {
        return "URL";
     }
}
