package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
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
public class AdsFetcher implements IdBasedFetcher {

    private static final String URL_PATTERN = "http://adsabs.harvard.edu/doi/";

    private ImportFormatPreferences preferences;

    public AdsFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return "ADS from ADS-DOI";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ADS;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<BibEntry> result = Optional.empty();

        String key = identifier.replaceAll("^(doi:|DOI:)", "");

        try {
            URIBuilder uriBuilder = new URIBuilder(URL_PATTERN + key);
            uriBuilder.addParameter("data_type", "BIBTEXPLUS");
            URL url = uriBuilder.build().toURL();

            String bibtexString = Unirest.get(url.toString()).asString().getBody();

            if(bibtexString.contains("@")) {
                bibtexString = bibtexString.substring(bibtexString.indexOf('@'));
                result = BibtexParser.singleFromString(bibtexString, preferences);
            }

            if (result.isPresent()) {
                result = postProcess(result.get());
            }

        } catch (MalformedURLException | UnirestException | URISyntaxException e) {
            throw new FetcherException("Error fetching ADS", e);
        }
        return result;
    }

    private Optional<BibEntry> postProcess(BibEntry entry){
        //TODO: Remove useless brackets
        String titleString = entry.getField(FieldName.TITLE).get();
        String abstractString = entry.getField(FieldName.ABSTRACT).get();
        String authorString = entry.getField(FieldName.AUTHOR).get();

        return Optional.of(entry);
    }

}
