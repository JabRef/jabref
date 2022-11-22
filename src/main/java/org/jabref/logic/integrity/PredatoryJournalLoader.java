package org.jabref.logic.integrity;

import java.io.IOException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.net.URI;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredatoryJournalLoader
{
    private static class PJSource
    {
        final String    URL;
        final String    ELEMENT_REGEX;
        final String[]  BIB_FIELDS;

        PJSource(String URL, String ELEMENT_REGEX, String... BIB_FIELDS)
        {
            this.URL                    = URL;
            this.ELEMENT_REGEX          = ELEMENT_REGEX;
            this.BIB_FIELDS             = new String[BIB_FIELDS.length];

            System.arraycopy(BIB_FIELDS, 0, this.BIB_FIELDS, 0, BIB_FIELDS.length);
        }
    }

    private static final List<PJSource> PREDATORY_SOURCES = List.of(
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/journals.csv",
                    null,
                    null,
                    "journal", "journalname", "bookname"),
        /*
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/hijacked.csv",
                    null,
                    null,
                    "journal", "journalname", "bookname"),
        */
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/publishers.csv",
                    null,
                    null,
                    "publisher"),
        new PJSource("https://beallslist.net/",
                    "<li>.*?</li>",
                    "publisher"),
        new PJSource("https://beallslist.net/standalone-journals/",
                    "<li>.*?</li>",
                    "journal", "journalname", "bookname"),
        new PJSource("https://beallslist.net/hijacked-journals/",
                    "<tr>.*?</tr>",
                    "journal", "journalname", "bookname")
    );

    private static final Logger                 LOGGER              = LoggerFactory.getLogger(PredatoryJournalLoader.class);
    private static HttpClient                   client;
    private static List<String>                 linkElements;
    private static MVStore                      mvStore;
    private static MVMap<String, List<String>>  predatoryJournals;

    public PredatoryJournalLoader()
    {
        this.client             = HttpClient.newHttpClient();
        this.linkElements       = new ArrayList<>();
        this.mvStore            = MVStore.open(null);                       // if fileName is null, store is in-memory
        this.predatoryJournals  = mvStore.openMap("predatoryJournals");
    }

    public static void load()
    {
        PREDATORY_SOURCES   .forEach(PredatoryJournalLoader::crawl);        // populates linkElements (and predatoryJournals if CSV)
        linkElements        .forEach(PredatoryJournalLoader::clean);        // adds cleaned HTML to predatoryJournals

        LOGGER.info("LOADED PREDATORY JOURNAL LIST");
    }

    public static MVMap getMap() { return predatoryJournals; }

    private static void crawl(PJSource source)
    {
        var uri     = URI.create(source.URL);
        var request = HttpRequest.newBuilder().uri(uri).build();

        try
        {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            if (response.statusCode() != 200)       { LOGGER.warn("BAD RESPONSE"); }
            else if (source.URL.contains(".csv"))   { handleCSV(response.body()); }
            else                                    { handleHTML(source.ELEMENT_REGEX, response.body()); }
        }
        catch (IOException ex)          { logException(ex); }
        catch (InterruptedException ex) { logException(ex); }
    }

    private static void handleCSV(String body)
    {
        var csvSplit = Pattern.compile("(\"[^\"]*\"|[^,]+)");

        for (String line : body.split("\n"))                            // TODO: skip header
        {
            var matcher = csvSplit.matcher(line);
            String[] cells = new String[3];

            for (int i = 0; matcher.find() && i < 3; i++) cells[i] = matcher.group();

            addToPredatoryJournals(cells[1], cells[2], cells[0]);       // change column order from CSV (source: url, name, abbr)
        }

    }

    private static void handleHTML(String regex, String body)
    {
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(body);

        while (matcher.find()) linkElements.add(matcher.group());
    }

    private static void clean(String item)
    {
        var p_name = Pattern.compile("(?<=\">).*?(?=<)");
        var p_url  = Pattern.compile("http.*?(?=\")");
        var p_abbr = Pattern.compile("(?<=\\()[^\s]*(?=\\))");

        var m_name = p_name.matcher(item);
        var m_url  = p_url.matcher(item);
        var m_abbr = p_abbr.matcher(item);

        // using `if` gets only first link in element, `while` gets all, but this may not be desirable
        // e.g. this way only the hijacked journals are recorded and not the authentic originals
        if (m_name.find() && m_url.find()) addToPredatoryJournals(m_name.group(), m_abbr.find() ? m_abbr.group() : "", m_url.group());
    }

    private static void addToPredatoryJournals(String name, String abbr, String url)
    {
        // computeIfAbsent -- more efficient if key is already present as list only created if absent
        // predatoryJournals.computeIfAbsent(decode(name), (k, v) -> new ArrayList<String>()).addAll(List.of(decode(abbr), url));

        predatoryJournals.put(decode(name), List.of(decode(abbr), url));
    }

    private static String decode(String s)
    {
        if (s == null) return "";

        return s.replace(",", "")
                .replace("&amp;", "")
                .replace("&#8217;", "'")
                .replace("&#8211;", "-");
    }

    private static void logException(Exception ex) {
        if (LOGGER.isErrorEnabled()) LOGGER.error(ex.getMessage(), ex);
    }
}
