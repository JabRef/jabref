package org.jabref.logic.journals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jabref.logic.net.URLDownload;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredatoryJournalLoader {
    private static class PJSource {
        URL URL = null;
        final String ELEMENT_REGEX;

        PJSource(String URL, String ELEMENT_REGEX) {
            try {
                this.URL = new URL(URL);
            } catch (MalformedURLException ex) {
                PredatoryJournalLoader.logException(ex);
            }
            this.ELEMENT_REGEX = ELEMENT_REGEX;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalLoader.class);
    private static final List<PJSource> PREDATORY_SOURCES = List.of(
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/journals.csv",
                    null),
        /*
        new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/hijacked.csv",
                    null),
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
    private static PredatoryJournalRepository repository;
    private static List<String> linkElements = new ArrayList<>();

    public static PredatoryJournalRepository loadRepository(boolean doUpdate) {
        // Initialize in-memory repository
        repository = new PredatoryJournalRepository();

        // Update from external sources
        if (doUpdate) {
            update();
        }

        return repository;
    }

    private static void update() {
        PREDATORY_SOURCES.forEach(PredatoryJournalLoader::crawl);       // populates linkElements (and predatoryJournals if CSV)
        linkElements.forEach(PredatoryJournalLoader::clean);            // adds cleaned HTML to predatoryJournals

        LOGGER.info("UPDATED PREDATORY JOURNAL LIST");
    }

    private static void crawl(PJSource source) {
        try {
            URLDownload download = new URLDownload(source.URL);

            if (!download.canBeReached()) {
                LOGGER.warn("URL UNREACHABLE");
            } else if (source.URL.getPath().contains(".csv")) {
                handleCSV(new InputStreamReader(download.asInputStream()));
            } else {
                handleHTML(source.ELEMENT_REGEX, download.asString());
            }
        } catch (IOException ex) {
            logException(ex);
        }
    }

    private static void handleCSV(Reader reader) throws IOException {
        CSVParser csvParser = new CSVParser(reader, CSVFormat.EXCEL);

        for (CSVRecord csvRecord : csvParser) {
            // changes column order from CSV (source: url, name, abbr)
            repository.addToPredatoryJournals(csvRecord.get(1), csvRecord.get(2), csvRecord.get(0));
        }
    }

    private static void handleHTML(String regex, String body) {
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(body);

        while (matcher.find()) {
            linkElements.add(matcher.group());
        }
    }

    private static void clean(String item) {
        var m_name = Pattern.compile("(?<=\">).*?(?=<)").matcher(item);
        var m_url = Pattern.compile("http.*?(?=\")").matcher(item);
        var m_abbr = Pattern.compile("(?<=\\()[^\s]*(?=\\))").matcher(item);

        // using `if` gets only first link in element, `while` gets all, but this may not be desirable
        // e.g. this way only the hijacked journals are recorded and not the authentic originals
        if (m_name.find() && m_url.find()) {
            repository.addToPredatoryJournals(m_name.group(), m_abbr.find() ? m_abbr.group() : "", m_url.group());
        }
    }

    private static void logException(Exception ex) {
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }
}
