package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.logic.importer.fetcher.LOBIDFetcher;

public class LOBIDIsbnFetcher extends AbstractIsbnFetcher {

    private final LOBIDFetcher lobidFetcher = new LOBIDFetcher();

    public LOBIDIsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        this.ensureThatIsbnIsValid(identifier);
        return lobidFetcher.getUrlForIdentifier(identifier);
    }

    @Override
    public String getName() {
        return lobidFetcher.getName();
    }

    @Override
    public Parser getParser() {
        return lobidFetcher.getParser();
    }
}
