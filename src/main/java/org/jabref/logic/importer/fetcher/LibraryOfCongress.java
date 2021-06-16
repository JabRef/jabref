package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.ModsImporter;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for the Library of Congress Control Number (LCCN) using https://lccn.loc.gov/
 */
public class LibraryOfCongress implements IdBasedParserFetcher {

    private final ImportFormatPreferences importFormatPreferences;

    public LibraryOfCongress(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "Library of Congress";
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://lccn.loc.gov/" + identifier + "/mods");
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new ModsImporter(this.importFormatPreferences);
    }
}
