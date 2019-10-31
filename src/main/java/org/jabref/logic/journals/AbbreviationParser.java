package org.jabref.logic.journals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.strings.StringUtil;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads abbreviation files (CSV format) into a list of Abbreviations.
 */
public class AbbreviationParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationParser.class);

    private final Set<Abbreviation> abbreviations = new HashSet<>(5000);

    public void readJournalListFromResource(String resourceFileName) {
        try {
            URL url = Objects.requireNonNull(JournalAbbreviationRepository.class.getResource(Objects.requireNonNull(resourceFileName)));
            readJournalList(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.info(String.format("Could not read journal list from file %s", resourceFileName), e);
        }
    }

    public void readJournalListFromFile(File file) throws FileNotFoundException {
        try (FileInputStream stream = new FileInputStream(Objects.requireNonNull(file));
             InputStreamReader reader = new InputStreamReader(stream, Objects.requireNonNull(StandardCharsets.UTF_8))) {
            readJournalList(reader);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn(String.format("Could not read journal list from file %s", file.getAbsolutePath()), e);
        }
    }

    public void readJournalListFromFile(File file, Charset encoding) throws FileNotFoundException {
        try (FileInputStream stream = new FileInputStream(Objects.requireNonNull(file));
             InputStreamReader reader = new InputStreamReader(stream, Objects.requireNonNull(encoding))) {
            readJournalList(reader);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn(String.format("Could not read journal list from file %s", file.getAbsolutePath()), e);
        }
    }

    /**
     * Read the given file, which should contain a list of journal names and their abbreviations. Each line should be
     * formatted as: "Full Journal Name;Abbr. Journal Name;[Shortest Unique Abbreviation]"
     *
     * @param reader a given file into a Reader object
     */
    private void readJournalList(Reader reader) {
        try (CSVParser csvParser = new CSVParser(reader, AbbreviationFormat.getCSVFormat())) {
            for (CSVRecord csvRecord : csvParser) {
                String name = csvRecord.size() > 0 ? csvRecord.get(0) : StringUtil.EMPTY;
                String abbreviation = csvRecord.size() > 1 ? csvRecord.get(1) : StringUtil.EMPTY;
                String shortestUniqueAbbreviation = csvRecord.size() > 2 ? csvRecord.get(2) : StringUtil.EMPTY;

                // Check name and abbreviation
                if (name.isEmpty() || abbreviation.isEmpty()) {
                    return;
                }

                abbreviations.add(new Abbreviation(name, abbreviation, shortestUniqueAbbreviation));
            }
        } catch (IOException ex) {
            LOGGER.info("Could not read journal list from file ", ex);
        }
    }

    public List<Abbreviation> getAbbreviations() {
        return new LinkedList<>(abbreviations);
    }
}
