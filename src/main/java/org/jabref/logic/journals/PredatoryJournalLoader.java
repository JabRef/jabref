package org.jabref.logic.journals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.OS;

import net.harawata.appdirs.AppDirsFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredatoryJournalLoader {
    private static class PJSource {
        URL url = null;
        final Pattern elementPattern;

        PJSource(String url, String regex) {
            try {
                this.url = new URL(url);
            } catch (MalformedURLException ex) {
                LOGGER.error("Malformed URL has occurred in PJSource", ex);
            }
            this.elementPattern = (regex != null) ? Pattern.compile(regex) : null;
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalLoader.class);
    private static final Pattern PATTERN_NAME = Pattern.compile("(?<=\">).*?(?=<)");
    private static final Pattern PATTERN_URL = Pattern.compile("http.*?(?=\")");
    private static final Pattern PATTERN_ABBR = Pattern.compile("(?<=\\()[^ ]*(?=\\))");
    private static final List<String> LINK_ELEMENTS = new ArrayList<>();
    private static final List<PJSource> PREDATORY_SOURCES = List.of(
            new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/journals.csv",
                    null),
            new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/publishers.csv",
                    null),
            new PJSource("https://beallslist.net/",
                    "<li>.*?</li>"),
            new PJSource("https://beallslist.net/standalone-journals/",
                    "<li>.*?</li>"),
            new PJSource("https://beallslist.net/hijacked-journals/",
                    "<tr>.*?</tr>")
    );
    private PredatoryJournalRepository repository = new PredatoryJournalRepository();

    public static PredatoryJournalRepository loadRepository() {
        PredatoryJournalLoader loader = new PredatoryJournalLoader();
        // Initialize with built-in list
        try (InputStream resourceAsStream = PredatoryJournalRepository.class.getResourceAsStream("/journals/predatoryJournal-list.mv")) {
            if (resourceAsStream == null) {
                LOGGER.warn("There is no predatoryJournalList.mv. We use a default predatory journal list");
            } else {
                // Use user's app data directory for more permanent storage
                Path appDataDir = Path.of(AppDirsFactory.getInstance()
                                                        .getUserDataDir(
                                                                OS.APP_DIR_APP_NAME,
                                                                "predatoryJournals",
                                                                OS.APP_DIR_APP_AUTHOR));
                Files.createDirectories(appDataDir); // Ensure the directory exists
                Path predatoryJournalListPath = appDataDir.resolve("predatoryJournal-list.mv");
                Files.copy(resourceAsStream, predatoryJournalListPath, StandardCopyOption.REPLACE_EXISTING);
                loader.repository = new PredatoryJournalRepository(predatoryJournalListPath);
            }
        } catch (IOException e) {
            LOGGER.error("Error while copying predatory journal list", e);
            return null;
        }
        return loader.repository;
    }

    public void update() {
        // populates linkElements (and predatoryJournals if CSV)
        PREDATORY_SOURCES.forEach(this::crawl);
        // adds cleaned HTML to predatoryJournals
        LINK_ELEMENTS.forEach(this::clean);

        LOGGER.info("Updated predatory journal list");
    }

    private void crawl(PJSource source) {
        try {
            URLDownload download = new URLDownload(source.url);

            if (!download.canBeReached()) {
                LOGGER.warn("URL UNREACHABLE");
            } else if (source.url.getPath().contains(".csv")) {
                handleCSV(new InputStreamReader(download.asInputStream()));
            } else {
                handleHTML(source.elementPattern, download.asString());
            }
        } catch (Exception ex) {
            LOGGER.error("Could not crawl source {}", source.url, ex);
        }
    }

    private void handleCSV(Reader reader) throws IOException {
        CSVParser csvParser = new CSVParser(reader, CSVFormat.EXCEL);

        for (CSVRecord csvRecord : csvParser) {
            // changes column order from CSV (source: url, name, abbr)
            repository.addToPredatoryJournals(csvRecord.get(1), csvRecord.get(2), csvRecord.get(0));
        }
    }

    private void handleHTML(Pattern pattern, String body) {
        Matcher matcher = (pattern != null) ? pattern.matcher(body) : null;

        if (matcher != null) {
            while (matcher.find()) {
                LINK_ELEMENTS.add(matcher.group());
            }
        }
    }

    private void clean(String item) {
        Matcher m_name = PATTERN_NAME.matcher(item);
        Matcher m_url = PATTERN_URL.matcher(item);
        Matcher m_abbr = PATTERN_ABBR.matcher(item);

        // using `if` gets only first link in element, `while` gets all, but this may not be desirable
        // e.g. this way only the hijacked journals are recorded and not the authentic originals
        if (m_name.find() && m_url.find()) {
            repository.addToPredatoryJournals(m_name.group(), m_abbr.find() ? m_abbr.group() : "", m_url.group());
        }
    }

    public PredatoryJournalRepository getRepository() {
        return repository;
    }
}
