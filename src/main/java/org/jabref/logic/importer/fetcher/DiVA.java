package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.BibtexParser;

import org.apache.hc.core5.net.URIBuilder;

/*
 * http://www.diva-portal.org/smash/aboutdiva.jsf?dswid=-3222
 * DiVA portal contains research publications and student theses from 40 Swedish universities and research institutions.
 */
public class DiVA implements IdBasedParserFetcher {

    private final ImportFormatPreferences importFormatPreferences;

    public DiVA(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "DiVA";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_DIVA);
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder("http://www.diva-portal.org/smash/getreferences");

        uriBuilder.addParameter("referenceFormat", "BibTex");
        uriBuilder.addParameter("pids", identifier);

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

    public boolean isValidId(String identifier) {
        return identifier.startsWith("diva2:");
    }
}
