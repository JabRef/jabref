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

            if (bibtexString.contains("@")) {
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

    /**
     * Remove all useless curly brackets in the given entry.
     * The original fetcher fetches a bibtex file containing "{ and }" at the end of an abstract or title. This causes a {{ and }} in JabRef.
     * @param entry Fetched entry with useless curly brackets
     * @return cleaned entry
     */
    private Optional<BibEntry> postProcess(BibEntry entry) {
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
        return Optional.of(entry);
    }

}
