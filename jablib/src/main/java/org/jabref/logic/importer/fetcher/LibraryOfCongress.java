package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.ModsImporter;

import org.apache.hc.core5.net.URIBuilder;

/// Fetcher for the Library of Congress Control Number (LCCN) using https://lccn.loc.gov/
public class LibraryOfCongress implements IdBasedParserFetcher {

    private static final String API_URL = "http://lx2.loc.gov:210/LCDB";

    private final ImportFormatPreferences importFormatPreferences;

    public LibraryOfCongress(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "Library of Congress";
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", "bath.lccn=\"" + identifier + "\"");
        uriBuilder.addParameter("startRecord", "1");
        uriBuilder.addParameter("maximumRecords", "1");
        uriBuilder.addParameter("recordSchema", "mods");
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new ModsImporter(this.importFormatPreferences);
    }
}
