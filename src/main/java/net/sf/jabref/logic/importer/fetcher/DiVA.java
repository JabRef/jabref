package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.http.client.utils.URIBuilder;

/*
 * http://www.diva-portal.org/smash/aboutdiva.jsf?dswid=-3222
 * DiVA portal contains research publications and student theses from 40 Swedish universities and research institutions.
 */
public class DiVA implements IdBasedFetcher {

    private static final String URL = "http://www.diva-portal.org/smash/getreferences"; // ?referenceFormat=BibTex&pids=%s";

    private final ImportFormatPreferences importFormatPreferences;


    public DiVA(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "DiVA";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DIVA_TO_BIBTEX;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        try {
            URIBuilder uriBuilder = new URIBuilder(URL);

            uriBuilder.addParameter("referenceFormat", "BibTex");
            uriBuilder.addParameter("pids", identifier);

            URLDownload dl = new URLDownload(uriBuilder.build().toURL());

            String bibtexString = dl.downloadToString(StandardCharsets.UTF_8);
            return BibtexParser.singleFromString(bibtexString, importFormatPreferences);

        } catch (URISyntaxException | IOException e) {
            throw new FetcherException("Problem getting information from DiVA", e);
        }
    }

    public boolean isValidId(String identifier) {
        return identifier.startsWith("diva2:");
    }
}
