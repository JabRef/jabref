package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.ISBN;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for ISBN.
 */
public class IsbnFetcher implements IdBasedFetcher {

    private static final String URL_PATTERN = "http://www.ebook.de/de/tools/isbn2bibtex?";
    private ImportFormatPreferences prefs;

    public IsbnFetcher(ImportFormatPreferences prefs){
        this.prefs = prefs;
    }

    @Override
    public String getName() {
        return "ISBN";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ISBN_TO_BIBTEX;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        ISBN isbn = new ISBN(identifier);
        Optional<BibEntry> result = Optional.empty();

        if (isbn.isValidChecksum() && isbn.isValidFormat()) {
            try {
                //Build the URL. In this case: http://www.ebook.de/de/tools/isbn2bibtex?isbn=identifier
                URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
                uriBuilder.addParameter("isbn", identifier);
                URL url = uriBuilder.build().toURL();

                //Downloads the source code of the site and then creates a .bib file out of the String
                String bibtexString = Unirest.get(url.toString()).asString().getBody();
                Optional<BibEntry> entry = BibtexParser.singleFromString(bibtexString, prefs);

                if (entry.isPresent()) {
                    result = postProcessEntry(entry.get());
                }

            } catch (UnirestException | IOException | URISyntaxException e) {
                throw new FetcherException("Bad URL when fetching ISBN info", e);
            }
        }
        return result;
    }

    private Optional<BibEntry> postProcessEntry(BibEntry entry) {
        if (entry.hasField(FieldName.URL)) {
            entry.clearField(FieldName.URL);
        }

        //Removes every non-digit character in the PAGETOTAL field.
        Optional<String> pagetotal = entry.getField(FieldName.PAGETOTAL);
        pagetotal.ifPresent(pg -> {
            entry.setField(FieldName.PAGETOTAL, pg.replaceAll("[\\D]", ""));
        });

        return Optional.of(entry);
    }
}
