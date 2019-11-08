package org.jabref.logic.importer.fetcher;

import java.io.File;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This fetcher parses text format citations using the web page of text2bib (https://text2bib.economics.utoronto.ca/)
 */
public class Text2BibFetcher implements SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(Text2BibFetcher.class);
    private final ImportFormatPreferences importFormatPreferences;

    public Text2BibFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        UnirestInstance unirest = Unirest.primaryInstance(); // Unirest.spawnInstance();
        unirest.config().addInterceptor(
                (request, context) -> {
                    System.out.println(request);
                });

        unirest.config().enableCookieManagement(true);
        try {
            // get the session ID cookie "T2BSID"
            HttpResponse<String> loginPageResponse = unirest.get("https://text2bib.economics.utoronto.ca/index.php/login/login").asString();
            if (loginPageResponse.getStatus() != 200) {
                LOGGER.error("Could not open login page.");
                LOGGER.error(loginPageResponse.getStatusText());
                LOGGER.error(loginPageResponse.toString());
                throw new FetcherException("Could not open login page.");
            }
            HttpResponse<String> loginResponse = unirest
                    .post("https://text2bib.economics.utoronto.ca/index.php/login/signIn")
                    .header("Referer", "https://text2bib.economics.utoronto.ca/index.php/login/login")
                    .accept("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .field("source", "")
                    .field("username", "koppor")
                    .field("password", "IOAt7cS5zpUXQkXvRmk5")
                    .asString();
            if (loginResponse.getStatus() != 302) {
                LOGGER.error("Could not login.");
                LOGGER.error(loginPageResponse.getStatusText());
                LOGGER.error(loginPageResponse.getBody());
                throw new FetcherException("Could not login.");
            }
            HttpResponse<String> conversionResponse = unirest
                    .post("https://text2bib.economics.utoronto.ca/index.php/index/convert")
                    .header("Referer", "https://text2bib.economics.utoronto.ca/index.php/index")
                    .accept("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3")
                    .field("index", 0)
                    .field("uploadFile", new File("C:/temp/test.txt"))
                    .field("labelStyle", "long")
                    .field("lineEndings", "l")
                    .field("charEncoding", "utf8leave")
                    .field("language", "en")
                    .field("firstComponent", "author")
                    .field("itemSeparator", "cr")
                    .field("percentComment", "0")
                    .field("incremental", "0")
                    .field("citationUserGroupId", "0")
                    .field("debug", "0")
                    .field("B1", "")
                    .header("Cookie", "T2BSID=htfe807iogfrdbclco3lbqno61")
                    .asString();
            if (conversionResponse.getStatus() != 200) {
                LOGGER.error("Could not convert.");
                LOGGER.error(loginPageResponse.getStatusText());
                LOGGER.error(loginPageResponse.getBody());
                throw new FetcherException("Could not convert.");
            }
            String bibtexUrl = conversionResponse.getBody().replaceAll(".*<a href=\"https://text2bib.economics.utoronto.ca/index.php/index/download/(\\d+).*", "$1");
            if (bibtexUrl.length() > 200) {
                LOGGER.error("Could not extract bibtex url.");
                LOGGER.error(conversionResponse.getBody());
                throw new FetcherException("Could not fetch bibtex.");
            }
            HttpResponse<String> bibtexResponse = unirest.get(bibtexUrl).asString();
            if (bibtexResponse.getStatus() != 200) {
                LOGGER.error("Could not fetch bibtex from {}.", bibtexUrl);
                LOGGER.error(bibtexResponse.getStatusText());
                LOGGER.error(bibtexResponse.getBody());
                throw new FetcherException("Could not fetch bibtex.");
            }
            bibtexResponse.getBody();
            BibtexParser bibtexParser = new BibtexParser(this.importFormatPreferences, new DummyFileUpdateMonitor());
            List<BibEntry> bibEntries = null;
            try {
                bibEntries = bibtexParser.parseEntries(bibtexResponse.getBody());
            } catch (ParseException e) {
                LOGGER.error("Could parse result", e);
                throw new FetcherException("Could not parse result from online service");
            }
            return bibEntries;
        } finally {
            unirest.close();
        }
    }

    @Override
    public String getName() {
        return "text2bib";
    }
}
