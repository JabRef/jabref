package org.jabref.logic.journals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
 * Reads abbreviation files (property files using NAME = ABBREVIATION as a format) into a list of Abbreviations.
 */
public class AbbreviationParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationParser.class);

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
        try (FileReader reader = new FileReader(Objects.requireNonNull(file))) {
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
     * Read the given file, which should contain a list of journal names and their
     * abbreviations. Each line should be formatted as: "Full Journal Name=Abbr. Journal Name"
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
        String[] parts = line.split("=");
        if (parts.length == 2) {
            final String fullName = parts[0].trim();
            final String abbrName = parts[1].trim();

            // check
            if ((fullName.length() <= 0) || (abbrName.length() <= 0)) {
                return;
            }

            Abbreviation abbreviation = new Abbreviation(fullName, abbrName);
            this.abbreviations.add(abbreviation);
        }
    }

    public List<Abbreviation> getAbbreviations() {
        return new LinkedList<>(abbreviations);
    }
}
