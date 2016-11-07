package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.EntryBasedParserFetcher;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedParserFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParseException;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.SearchBasedParserFetcher;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;
import org.jsoup.helper.StringUtil;

/**
 * Fetches data from the SAO/NASA Astrophysics Data System (http://www.adsabs.harvard.edu/)
 *
 * Search query-based: http://adsabs.harvard.edu/basic_search.html
 * Entry -based: http://adsabs.harvard.edu/abstract_service.html
 *
 * There is also a new API (https://github.com/adsabs/adsabs-dev-api) but it returns JSON
 * (or at least needs multiple calls to get BibTeX, status: September 2016)
 */
public class AstrophysicsDataSystem implements IdBasedParserFetcher, SearchBasedParserFetcher, EntryBasedParserFetcher {

    private static String API_QUERY_URL = "http://adsabs.harvard.edu/cgi-bin/nph-basic_connect";
    private static String API_ENTRY_URL = "http://adsabs.harvard.edu/cgi-bin/nph-abs_connect";
    private static String API_DOI_URL = "http://adsabs.harvard.edu/doi/";

    private final String patternRemoveDOI = "^(doi:|DOI:)";
    private final ImportFormatPreferences preferences;

    public AstrophysicsDataSystem(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "SAO/NASA Astrophysics Data System";
    }

    private URIBuilder getBaseUrl(String apiUrl) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(apiUrl);
        uriBuilder.addParameter("data_type", "BIBTEXPLUS");
        uriBuilder.addParameter("start_nr", String.valueOf(1));
        uriBuilder.addParameter("nr_to_return", String.valueOf(200));
        return uriBuilder;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = getBaseUrl(API_QUERY_URL);
        uriBuilder.addParameter("qsearch", query);
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = getBaseUrl(API_ENTRY_URL);

        // Search astronomy + physics + arXiv db
        uriBuilder.addParameter("db_key", "AST");
        uriBuilder.addParameter("db_key", "PHY");
        uriBuilder.addParameter("db_key", "PRE");

        // Add title search
        entry.getFieldOrAlias(FieldName.TITLE).ifPresent(title -> {
            uriBuilder.addParameter("ttl_logic", "OR");
            uriBuilder.addParameter("title", title);
            uriBuilder.addParameter("ttl_syn", "YES"); // Synonym replacement
            uriBuilder.addParameter("ttl_wt", "0.3"); // Weight
            uriBuilder.addParameter("ttl_wgt", "YES"); // Consider Weight
        });

        // Add author search
        entry.getFieldOrAlias(FieldName.AUTHOR).ifPresent(author -> {
            uriBuilder.addParameter("aut_logic", "OR");
            uriBuilder.addParameter("author", author);
            uriBuilder.addParameter("aut_syn", "YES"); // Synonym replacement
            uriBuilder.addParameter("aut_wt", "1.0"); // Weight
            uriBuilder.addParameter("aut_wgt", "YES"); // Consider weight
        });

        return uriBuilder.build().toURL();
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        String key = identifier.replaceAll(patternRemoveDOI, "");
        URIBuilder uriBuilder = new URIBuilder(API_DOI_URL + key);
        uriBuilder.addParameter("data_type", "BIBTEXPLUS");
        return uriBuilder.build().toURL();
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ADS;
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences);
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        try {
            URLConnection connection = getURLForQuery(query).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");
            try(InputStream stream = connection.getInputStream()) {
                List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

                // Post-cleanup
                fetchedEntries.forEach(this::doPostCleanup);
                return fetchedEntries;
            } catch (IOException e) {
                throw new FetcherException("An I/O exception occurred", e);
            }
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            throw new FetcherException("An I/O exception occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("Error occurred when parsing entry", Localization.lang("Error occurred when parsing entry"), e);
        }
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(FieldName.ABSTRACT, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(FieldName.TITLE, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(FieldName.AUTHOR, new NormalizeNamesFormatter()).cleanup(entry);

        // Remove url to ADS page
        new FieldFormatterCleanup("adsnote", new ClearFormatter()).cleanup(entry);
        new FieldFormatterCleanup("adsurl", new ClearFormatter()).cleanup(entry);
    }
}
