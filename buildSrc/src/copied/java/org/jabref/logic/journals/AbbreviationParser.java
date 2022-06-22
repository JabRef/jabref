package org.jabref.logic.journals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        try (InputStream stream = JournalAbbreviationRepository.class.getResourceAsStream(resourceFileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            readJournalList(reader);
        } catch (IOException e) {
            LOGGER.error(String.format("Could not read journal list from file %s", resourceFileName), e);
        }
    }

    public void readJournalListFromFile(Path file) throws IOException {
        readJournalListFromFile(file, StandardCharsets.UTF_8);
    }

    public void readJournalListFromFile(Path file, Charset encoding) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file, encoding)) {
            readJournalList(reader);
        }
    }

    /**
     * Read the given file, which should contain a list of journal names and their abbreviations. Each line should be
     * formatted as: "Full Journal Name;Abbr. Journal Name;[Shortest Unique Abbreviation]"
     *
     * @param reader a given file into a Reader object
     */
    private void readJournalList(Reader reader) throws IOException {
        try (CSVParser csvParser = new CSVParser(reader, AbbreviationFormat.getCSVFormat())) {
            for (CSVRecord csvRecord : csvParser) {
                String name = csvRecord.size() > 0 ? csvRecord.get(0) : "";
                String abbreviation = csvRecord.size() > 1 ? csvRecord.get(1) : "";
                String shortestUniqueAbbreviation = csvRecord.size() > 2 ? csvRecord.get(2) : "";

                // Check name and abbreviation
                if (name.isEmpty() || abbreviation.isEmpty()) {
                    return;
                }

                abbreviations.add(new Abbreviation(name, abbreviation, shortestUniqueAbbreviation));
            }
        }
    }

    public List<Abbreviation> getAbbreviations() {
        return new LinkedList<>(abbreviations);
    }
}
