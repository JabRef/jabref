package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jsoup.helper.StringUtil;

/**
 * Fetcher for ISBN using https://bibtex.chimbori.com/, which in turn uses Amazon's API.
 */
public class IsbnViaChimboriFetcher extends AbstractIsbnFetcher {

    public IsbnViaChimboriFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "ISBN (Chimbori/Amazon)";
    }

    /**
     * @return null, because the identifier is passed using form data. This method is not used.
     */
    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        return null;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        this.ensureThatIsbnIsValid(identifier);

        HttpResponse<String> postResponse;
        try {
            postResponse = Unirest.post("https://bibtex.chimbori.com/isbn-bibtex")
                    .field("isbn", identifier)
                    .asString();
        } catch (UnirestException e) {
            throw new FetcherException("Could not retrieve data from chimbori.com", e);
        }
        if (postResponse.getStatus() != 200) {
            throw new FetcherException("Error while retrieving data from chimbori.com: " + postResponse.getBody());
        }

        List<BibEntry> fetchedEntries;
        try {
            fetchedEntries = getParser().parseEntries(postResponse.getRawBody());
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }
        if (fetchedEntries.isEmpty()) {
            return Optional.empty();
        } else if (fetchedEntries.size() > 1) {
            LOGGER.info("Fetcher " + getName() + "found more than one result for identifier " + identifier
                    + ". We will use the first entry.");
        }

        BibEntry entry = fetchedEntries.get(0);

        // chimbori does not return an ISBN. Thus, we add the one searched for
        entry.setField("isbn", identifier);

        doPostCleanup(entry);

        return Optional.of(entry);
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // We MUST NOT clean the URL. this is the deal with @manastungare - see https://github.com/JabRef/jabref/issues/684#issuecomment-266541507
        // DO NOT add following code:
        // new FieldFormatterCleanup(FieldName.URL, new ClearFormatter()).cleanup(entry);
    }

}
