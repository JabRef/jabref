package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.http.client.utils.URIBuilder;

/*
 * http://www.diva-portal.org/smash/aboutdiva.jsf?dswid=-3222
 * DiVA portal contains research publications and student theses from 40 Swedish universities and research institutions.
 */
public class DiVA implements IdBasedFetcher {

    private static final String URL = "http://www.diva-portal.org/smash/getreferences"; // ?referenceFormat=BibTex&pids=%s";


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
        Optional<BibEntry> result = Optional.empty();
        try {
            URIBuilder uriBuilder = new URIBuilder(URL);

            uriBuilder.addParameter("referenceFormat", "BibTex");
            uriBuilder.addParameter("pids", identifier);

            String bibtexString;
            URLDownload dl = new URLDownload(uriBuilder.build().toURL());

            bibtexString = dl.downloadToString(StandardCharsets.UTF_8);
            result = Optional.ofNullable(BibtexParser.singleFromString(bibtexString));

        } catch (URISyntaxException | IOException e) {
            throw new FetcherException("Problem getting information from DiVA", e);
        }

        return result;

    }

}
