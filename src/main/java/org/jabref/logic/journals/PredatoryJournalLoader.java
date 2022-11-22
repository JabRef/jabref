package org.jabref.logic.journals;

import java.io.IOException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.net.URI;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredatoryJournalLoader {
    private static class PJSource {
        final String    URL;
        final String    ELEMENT_REGEX;

        PJSource(String URL, String ELEMENT_REGEX) {
            this.URL            = URL;
            this.ELEMENT_REGEX  = ELEMENT_REGEX;
        }
    }

    private static final List<PJSource> PREDATORY_SOURCES = List.of(
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/journals.csv",
                    null),
        /*
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/hijacked.csv",
                    null,
                    null,
                    "journal", "journalname", "bookname"),
        */
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/publishers.csv",
                    null),
        new PJSource("https://beallslist.net/",
                    "<li>.*?</li>"),
        new PJSource("https://beallslist.net/standalone-journals/",
                    "<li>.*?</li>"),
        new PJSource("https://beallslist.net/hijacked-journals/",
                    "<tr>.*?</tr>")
    );

    private static final Logger                 LOGGER              = LoggerFactory.getLogger(PredatoryJournalLoader.class);
    private static HttpClient                   client;
    private static List<String>                 linkElements;
    private static PredatoryJournalRepository   repository;

    public PredatoryJournalLoader() {
        this.client             = HttpClient.newHttpClient();
        this.linkElements       = new ArrayList<>();
    }

    public static PredatoryJournalRepository loadRepository() {
        // Initialize in-memory repository
        repository = new PredatoryJournalRepository();

        // Update from external sources
        update();

        return repository;
    }

    public static void update() {
        PREDATORY_SOURCES   .forEach(PredatoryJournalLoader::crawl);            // populates linkElements (and predatoryJournals if CSV)
        linkElements        .forEach(PredatoryJournalLoader::clean);            // adds cleaned HTML to predatoryJournals

        LOGGER.info("UPDATED PREDATORY JOURNAL LIST");
    }

    private static void crawl(PJSource source) {
        var uri     = URI.create(source.URL);
        var request = HttpRequest.newBuilder().uri(uri).build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            if (response.statusCode() != 200)       { LOGGER.warn("BAD RESPONSE"); }
            else if (source.URL.contains(".csv"))   { handleCSV(response.body()); }
            else                                    { handleHTML(source.ELEMENT_REGEX, response.body()); }
        }
        catch (IOException ex)          { logException(ex); }
        catch (InterruptedException ex) { logException(ex); }
    }

    private static void handleCSV(String body) {
        var csvSplit = Pattern.compile("(\"[^\"]*\"|[^,]+)");

        for (String line : body.split("\n")) {                                  // TODO: skip header
            var matcher = csvSplit.matcher(line);
            String[] cells = new String[3];

            for (int i = 0; matcher.find() && i < 3; i++) cells[i] = matcher.group();

            repository.addToPredatoryJournals(cells[1], cells[2], cells[0]);    // change column order from CSV (source: url, name, abbr)
        }

    }

    private static void handleHTML(String regex, String body) {
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(body);

        while (matcher.find()) linkElements.add(matcher.group());
    }

    private static void clean(String item) {
        var p_name = Pattern.compile("(?<=\">).*?(?=<)");
        var p_url  = Pattern.compile("http.*?(?=\")");
        var p_abbr = Pattern.compile("(?<=\\()[^\s]*(?=\\))");

        var m_name = p_name.matcher(item);
        var m_url  = p_url.matcher(item);
        var m_abbr = p_abbr.matcher(item);

        // using `if` gets only first link in element, `while` gets all, but this may not be desirable
        // e.g. this way only the hijacked journals are recorded and not the authentic originals
        if (m_name.find() && m_url.find()) repository.addToPredatoryJournals(m_name.group(), m_abbr.find() ? m_abbr.group() : "", m_url.group());
    }

    private static void logException(Exception ex) { if (LOGGER.isErrorEnabled()) LOGGER.error(ex.getMessage(), ex); }
}
