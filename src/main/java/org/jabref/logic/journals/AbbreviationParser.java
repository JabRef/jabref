package org.jabref.logic.journals;

import java.io.BufferedReader;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads abbreviation files (CSV format) into a list of Abbreviations.
 */
public class AbbreviationParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationParser.class);

    private static final String separator = "\t";
    private static final String textQuote = "\"";

    private final Set<Abbreviation> abbreviations = new HashSet<>(5000);

    public void readJournalListFromResource(String resourceFileName) {
        URL url = Objects.requireNonNull(JournalAbbreviationRepository.class.getResource(Objects.requireNonNull(resourceFileName)));
        try {
            readJournalList(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.info("Could not read journal list from file " + resourceFileName, e);
        }
    }

    public void readJournalListFromFile(File file) throws FileNotFoundException {
        try (FileInputStream stream = new FileInputStream(Objects.requireNonNull(file));
             InputStreamReader reader = new InputStreamReader(stream, Objects.requireNonNull(StandardCharsets.UTF_8))) {
            readJournalList(reader);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn("Could not read journal list from file " + file.getAbsolutePath(), e);
        }
    }

    public void readJournalListFromFile(File file, Charset encoding) throws FileNotFoundException {
        try (FileInputStream stream = new FileInputStream(Objects.requireNonNull(file));
                InputStreamReader reader = new InputStreamReader(stream, Objects.requireNonNull(encoding))) {
            readJournalList(reader);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn("Could not read journal list from file " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Read the given file, which should contain a list of journal names and their abbreviations.
     * Each line should be formatted as: "Full Journal Name\tAbbr. Journal Name\t[Shortest Unique Name]"
     *
     * @param in
     */
    private void readJournalList(Reader in) {
        try (BufferedReader reader = new BufferedReader(in)) {
            String line;
            while ((line = reader.readLine()) != null) {
                addLine(line);
            }

        } catch (IOException ex) {
            LOGGER.info("Could not read journal list from file ", ex);
        }
    }

    private void addLine(String line) {
        if (line.startsWith("#")) {
            return;
        }

        String[] parts = line.replace(textQuote + textQuote, textQuote).split(separator);
        if (parts.length > 1) {
            String fullName = parts[0].trim();
            String abbrName = parts[1].trim();

            String shortestName = "";
            if (parts.length > 2) {
                shortestName = parts[2].trim();
            }

            // Check name and abbreviation
            if ((fullName.length() <= 0) || (abbrName.length() <= 0)) {
                return;
            }

            // Clean text quotes
            if (fullName.startsWith(textQuote) && fullName.endsWith(textQuote)) {
                fullName = fullName.substring(1, fullName.length() - 1);
            }

            if (abbrName.startsWith(textQuote) && abbrName.endsWith(textQuote)) {
                abbrName = abbrName.substring(1, abbrName.length() - 1);
            }

            if (shortestName.startsWith(textQuote) && shortestName.endsWith(textQuote)) {
                shortestName = shortestName.substring(1, shortestName.length() - 1);
            }

            Abbreviation abbreviation = new Abbreviation(fullName, abbrName, shortestName);
            this.abbreviations.add(abbreviation);
        }
    }

    public List<Abbreviation> getAbbreviations() {
        return new LinkedList<>(abbreviations);
    }
}
