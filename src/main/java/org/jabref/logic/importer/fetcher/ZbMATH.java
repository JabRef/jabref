package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.MoveFieldCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches data from the Zentralblatt Math (https://www.zbmath.org/)
 */
public class ZbMATH implements SearchBasedParserFetcher, IdBasedParserFetcher, EntryBasedParserFetcher {

    private final ImportFormatPreferences preferences;

    public ZbMATH(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "zbMATH";
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        Optional<String> zblidInEntry = entry.getField(StandardField.ZBL_NUMBER);
        if (zblidInEntry.isPresent()) {
            // zbmath id is already present
            System.out.println("Zbl number is present.");
            return getUrlForIdentifier(zblidInEntry.get());
        }

        URIBuilder uriBuilder = new URIBuilder("https://zbmath.org/citationmatching/match");
        uriBuilder.addParameter("n", "1"); // return only the best matching entry
        uriBuilder.addParameter("m", "5"); // return only entries with a score of at least 5

        entry.getFieldOrAlias(StandardField.TITLE).ifPresent(title -> uriBuilder.addParameter("t", title));
        entry.getFieldOrAlias(StandardField.JOURNAL).ifPresent(journal -> uriBuilder.addParameter("j", journal));
        entry.getFieldOrAlias(StandardField.YEAR).ifPresent(year -> uriBuilder.addParameter("y", year));
        entry.getFieldOrAlias(StandardField.PAGINATION)
             .ifPresent(pagination -> uriBuilder.addParameter("p", pagination));
        entry.getFieldOrAlias(StandardField.VOLUME).ifPresent(volume -> uriBuilder.addParameter("v", volume));
        entry.getFieldOrAlias(StandardField.ISSUE).ifPresent(issue -> uriBuilder.addParameter("i", issue));

        if (entry.getFieldOrAlias(StandardField.AUTHOR).isPresent()) {
            // replace "and" by ";" as citation matching API uses ";" for separation
            String author = entry.getFieldOrAlias(StandardField.AUTHOR).get();
            author = author.replace(" and ", ";");
            uriBuilder.addParameter("a", author);
            System.out.println("Author = "+author);
        }

        String urlString = uriBuilder.build().toString();
        System.out.println(urlString);
        HttpResponse<JsonNode> response = Unirest.get(urlString)
                                                 .asJson();
        String zblid = null;
        if (response.getStatus() == 200) {
            JSONArray result = response.getBody()
                                       .getObject()
                                       .getJSONArray("results");
            System.out.println(result.toString());
            if (result.length() > 0) {
                zblid = result.getJSONObject(0)
                              .get("zbl_id")
                              .toString();
            }
            if (zblid != null) {
                System.out.println("zbl_id = " + zblid);
            } else {
                System.out.println("No zbl_id found.");
            }
        }
        if (zblid == null) {
            // citation matching API found no entry
            // what should happen in this case?
            return null;
        } else {
            return getUrlForIdentifier(zblid);
        }
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://zbmath.org/bibtexoutput/");
        uriBuilder.addParameter("q", query); // search all fields
        uriBuilder.addParameter("start", "0"); // start index
        uriBuilder.addParameter("count", "200"); // should return up to 200 items (instead of default 100)

        URLDownload.bypassSSLVerification();

        return uriBuilder.build().toURL();
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://zbmath.org/bibtexoutput/");
        String query = "an:".concat(identifier); // use an: to search for a zbMATH identifier
        uriBuilder.addParameter("q", query);
        uriBuilder.addParameter("start", "0"); // start index
        uriBuilder.addParameter("count", "1"); // return exactly one item

        URLDownload.bypassSSLVerification();

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences, new DummyFileUpdateMonitor());
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup(new UnknownField("msc2010"), StandardField.KEYWORDS).cleanup(entry);
        new MoveFieldCleanup(new UnknownField("fjournal"), StandardField.JOURNAL).cleanup(entry);
        new FieldFormatterCleanup(StandardField.JOURNAL, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);
    }
}
