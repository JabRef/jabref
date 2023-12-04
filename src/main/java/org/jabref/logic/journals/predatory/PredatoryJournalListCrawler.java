package org.jabref.logic.journals.predatory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.net.URLDownload;
import org.jabref.model.strings.StringUtil;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts (hard-coded) online resources to a set. {@link #loadFromOnlineSources} is the method containing the result.
 */
public class PredatoryJournalListCrawler {

    private record PJSource(URL url, Optional<Pattern> elementPattern) {
        PJSource(String url, String regex) {
            this(createURL(url), Optional.of(Pattern.compile(regex)));
        }

        PJSource(String url) {
            this(createURL(url), Optional.empty());
        }

        private static URL createURL(String urlString) {
            try {
                return new URI(urlString).toURL();
            } catch (MalformedURLException | URISyntaxException ex) {
                throw new IllegalArgumentException("Malformed URL has occurred in PJSource", ex);
            }
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalListCrawler.class);
    private static final Pattern PATTERN_NAME = Pattern.compile("(?<=\">).*?(?=<)");
    private static final Pattern PATTERN_URL = Pattern.compile("http.*?(?=\")");
    private static final Pattern PATTERN_ABBR = Pattern.compile("(?<=\\()[^ ]*(?=\\))");
    private final List<PJSource> predatorySources = List.of(
            new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/journals.csv"),
            new PJSource("https://raw.githubusercontent.com/stop-predatory-journals/stop-predatory-journals.github.io/master/_data/publishers.csv"),
            new PJSource("https://beallslist.net/",
                    "<li>.*?</li>"),
            new PJSource("https://beallslist.net/standalone-journals/",
                    "<li>.*?</li>"),
            new PJSource("https://beallslist.net/hijacked-journals/",
                    "<tr>.*?</tr>")
    );

    private final List<String> linkElements = new ArrayList<>();

    private final List<PredatoryJournalInformation> predatoryJournalInformation = new ArrayList<>();

    /**
     * Loads predatory journal information from online resources
     * This method should be only called once when building JabRef
     *
     * @return the set of journal information
     */
    public HashSet<PredatoryJournalInformation> loadFromOnlineSources() {
        predatorySources.forEach(this::crawl);
        linkElements.forEach(this::clean);
        return new HashSet<>(predatoryJournalInformation);
    }

    private void crawl(PJSource source) {
        try {
            URLDownload download = new URLDownload(source.url);

            if (!download.canBeReached()) {
                LOGGER.warn("Url {} is unreachable", source.url);
            } else if (source.url.getPath().contains(".csv")) {
                handleCSV(new InputStreamReader(download.asInputStream()));
            } else {
                if (source.elementPattern.isPresent()) {
                    handleHTML(source.elementPattern.get(), download.asString());
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Could not crawl source for predatory journals {}", source.url, ex);
        }
    }

    private void handleCSV(Reader reader) throws IOException {
        CSVFormat format = CSVFormat.EXCEL.builder().setSkipHeaderRecord(true).build();
        CSVParser csvParser = new CSVParser(reader, format);

        for (CSVRecord csvRecord : csvParser) {
            String name = csvRecord.get(1);
            String abbr = csvRecord.get(2);
            String url = csvRecord.get(0);

            if (StringUtil.isNullOrEmpty(name)) {
                if (!abbr.isEmpty()) {
                    name = abbr;
                } else {
                    continue;
                }
            }
            // changes column order from CSV (source: url, name, abbr)
            predatoryJournalInformation.add(new PredatoryJournalInformation(decode(name), decode(abbr), url));
        }
    }

    private void handleHTML(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            linkElements.add(matcher.group());
        }
    }

    private void clean(String item) {
        Matcher m_name = PATTERN_NAME.matcher(item);
        Matcher m_url = PATTERN_URL.matcher(item);
        Matcher m_abbr = PATTERN_ABBR.matcher(item);

        // using `if` gets only first link in element, `while` gets all, but this may not be desirable
        // e.g. this way only the hijacked journals are recorded and not the authentic originals
        if (m_name.find() && m_url.find()) {
            String name = m_name.group();
            if (name != null) {
                name = name.replace("\u200B", ""); // zero width space
            }
            String abbr = m_abbr.find() ? m_abbr.group() : "";
            String url = m_url.group();

            if (StringUtil.isNullOrEmpty(name)) {
                if (!abbr.isEmpty()) {
                    name = abbr;
                } else {
                    return;
                }
            }
            predatoryJournalInformation.add(new PredatoryJournalInformation(decode(name), decode(abbr), url));
        }
    }

    private String decode(String s) {
        return Optional.ofNullable(s)
                       .orElse("")
                       .replace(",", "")
                       .replace("&amp;", "&")
                       .replace("&#8217;", "'")
                       .replace("&#8211;", "-");
    }
}
