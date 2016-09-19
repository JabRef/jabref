package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedParserFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.client.utils.URIBuilder;

/**
 * This class handles accessing and obtaining BibTeX entry
 * from ADS(The NASA Astrophysics Data System).
 * Fetching using DOI(Document Object Identifier) is only supported.
 */
public class AdsFetcher implements IdBasedParserFetcher {

    private static final String URL_PATTERN = "http://adsabs.harvard.edu/doi/";

    private ImportFormatPreferences importFormatPreferences;

    public AdsFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "ADS-DOI";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ADS;
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        String key = identifier.replaceAll("^(doi:|DOI:)", "");
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN + key);
        uriBuilder.addParameter("data_type", "BIBTEXPLUS");
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

    @Override
    public void doPostCleanup(BibEntry entry){
        Optional<String> optTitleString = entry.getField(FieldName.TITLE);
        if (optTitleString.isPresent()) {
            String titleString = optTitleString.get();
            titleString = StringUtil.shaveString(titleString);
            entry.setField(FieldName.TITLE, titleString.trim());
        }

        Optional<String> optAuthorString = entry.getField(FieldName.AUTHOR);
        if (optAuthorString.isPresent()) {
            String authorString = optAuthorString.get();
            authorString = authorString.replace('{', ' ');
            authorString = authorString.replace('}', ' ');
            authorString = authorString.replace("  ", " ");
            entry.setField(FieldName.AUTHOR, authorString.trim());
        }

        Optional<String> optAbstractStirng = entry.getField(FieldName.ABSTRACT);
        if (optAbstractStirng.isPresent()) {
            String abstractString = optAbstractStirng.get();
            abstractString = StringUtil.shaveString(abstractString);
            entry.setField(FieldName.ABSTRACT, abstractString.trim());
        }
    }
}
