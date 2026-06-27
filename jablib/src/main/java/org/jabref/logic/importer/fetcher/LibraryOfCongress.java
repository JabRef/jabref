package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;

import org.apache.hc.core5.net.URIBuilder;

/// Fetcher for the Library of Congress Control Number (LCCN) using https://lccn.loc.gov/
public class LibraryOfCongress implements IdBasedParserFetcher {

    /// The Library of Congress SRU gateway exposes this database on port 210 without TLS.
    /// `URLDownload` uses the URL scheme as configured, so keep this endpoint explicit instead of silently
    /// implying HTTPS support that the service does not provide.
    private static final String SRU_API_URL = "http://lx2.loc.gov:210/LCDB";
    private static final Pattern LCCN_PATTERN = Pattern.compile("[A-Za-z]{0,3}\\d{8,10}");

    private final ImportFormatPreferences importFormatPreferences;

    public LibraryOfCongress(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "Library of Congress";
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<String> normalizedIdentifier = normalizeIdentifier(identifier);
        if (normalizedIdentifier.isEmpty()) {
            return Optional.empty();
        }

        return IdBasedParserFetcher.super.performSearchById(normalizedIdentifier.orElseThrow());
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(SRU_API_URL);
        uriBuilder.addParameter("version", "1.1");
        uriBuilder.addParameter("operation", "searchRetrieve");
        uriBuilder.addParameter("query", "bath.lccn=\"" + escapeCqlPhrase(identifier) + "\"");
        uriBuilder.addParameter("startRecord", "1");
        uriBuilder.addParameter("maximumRecords", "1");
        uriBuilder.addParameter("recordSchema", "mods");
        return uriBuilder.build().toURL();
    }

    private static Optional<String> normalizeIdentifier(String identifier) {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        String normalizedIdentifier = identifier.trim()
                                                .replace(" ", "")
                                                .replace("-", "");
        if (LCCN_PATTERN.matcher(normalizedIdentifier).matches()) {
            return Optional.of(normalizedIdentifier);
        }

        return Optional.empty();
    }

    static String escapeCqlPhrase(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"");
    }

    @Override
    public Parser getParser() {
        return new ModsImporter(this.importFormatPreferences);
    }
}
