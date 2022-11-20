import java.io.Writer;
import java.io.FileWriter;
import java.io.IOException;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.net.URI;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;


class PJSource
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

public class PJDownloader
{
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

    private static HttpClient                   client              = HttpClient.newHttpClient();
    private static List<String>                 linkElements        = new ArrayList<>();
    private static Map<String, List<String>>    predatoryJournals   = new LinkedHashMap<>();
    private static Logger                       logger              = Logger.getLogger("PJDownloader", null);
    private static Writer                       writer;

    public static void main(String[] args)
    {
        try { writer = new FileWriter("PJCache.csv"); }
        catch (IOException ex) { logException(ex); }

        write("url", List.of("name", "abbr"));                  // write csv header

        PREDATORY_SOURCES   .forEach(PJDownloader::crawl);      // populates linkElements
        linkElements        .forEach(PJDownloader::clean);      // populates predatoryJournals
        predatoryJournals   .forEach(PJDownloader::write);      // write to CSV

        try { writer.close(); }
        catch (IOException ex) { logException(ex); }
    }

    private static void crawl(PJSource source)
    {
        var uri     = URI.create(source.URL);
        var request = HttpRequest.newBuilder().uri(uri).build();

        try
        {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            if (response.statusCode() != 200)       { logger.warning("BAD RESPONSE"); }
            else if (source.URL.contains(".csv"))   { handleCSV(response.body()); }
            else                                    { handleHTML(source.ELEMENT_REGEX, response.body()); }
        }
        catch (IOException ex)          { logException(ex); }
        catch (InterruptedException ex) { logException(ex); }
    }

    private static void clean(String item)
    {
        var p_url  = Pattern.compile("http.*?(?=\")");
        var p_name = Pattern.compile("(?<=\">).*?(?=<)");
        var p_abbr = Pattern.compile("(?<=\\()[^\s]*(?=\\))");

        var m_url  = p_url.matcher(item);
        var m_name = p_name.matcher(item);
        var m_abbr = p_abbr.matcher(item);

        // using `if` gets only first link in element, `while` gets all, but this may not be desirable
        // e.g. this way only the hijacked journals are recorded and not the authentic originals
        if (m_url.find() && m_name.find()) addToPredatoryJournals(m_url.group(), m_name.group(), m_abbr.find() ? m_abbr.group() : "");
    }

    private static void handleCSV(String body)
    {
        logger.info("FOUND CSV");
        var csvSplit = Pattern.compile("(\"[^\"]*\"|[^,]+)");

        for (String line : body.split("\n"))                    // TODO: skip header
        {
            var matcher = csvSplit.matcher(line);
            String[] cells = new String[3];

            for (int i = 0; matcher.find() && i < 3; i++) cells[i] = matcher.group();
            addToPredatoryJournals(cells[0], cells[1], cells[2]);
        }

    }

    private static void handleHTML(String regex, String body)
    {
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(body);

        while (matcher.find()) linkElements.add(matcher.group());
    }

    private static void addToPredatoryJournals(String url, String name, String abbr)
    {
        // compute vs. computeIfAbsent -- the former supercedes the old key, which in this case is desirable as it will override the non-standard CSV
        predatoryJournals.compute(url, (k, v) -> new ArrayList<String>())
                         .addAll(List.of(decode(name), decode(abbr)));
    }

    private static void write(String url, List<String> attr)
    {
        var line = String.join(",", url, attr.get(0), attr.get(1));

        try { writer.write(line + "\n"); }
        catch (IOException ex) { logException(ex); }
    }

    private static String decode(String s)
    {
        if (s == null) return "";

        return s.replace(",", "")
                .replace("&amp;", "")
                .replace("&#8217;", "'")
                .replace("&#8211;", "-");
    }

    private static void logException(Exception ex)
    {
        if (logger.isLoggable(Level.SEVERE)) logger.log(Level.SEVERE, ex.getMessage(), ex);
    }
}
