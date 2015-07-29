package net.sf.jabref.journals.logic;

import java.io.*;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Reads abbreviation files (property files using NAME = ABBREVIATION as a format) into a list of Abbreviations.
 */
public class AbbreviationParser {

    private final List<Abbreviation> abbreviations = new LinkedList<>();

    public void readJournalListFromResource(String resourceFileName) {
        URL url = Objects.requireNonNull(JournalAbbreviationRepository.class.getResource(Objects.requireNonNull(resourceFileName)));
        try {
            readJournalList(new InputStreamReader(url.openStream()));
        } catch (IOException e) {
            // TODO logging
            e.printStackTrace();
        }
    }

    public void readJournalListFromFile(File file) throws FileNotFoundException {
        readJournalList(new FileReader(Objects.requireNonNull(file)));
    }

    /**
     * Read the given file, which should contain a list of journal names and their
     * abbreviations. Each line should be formatted as: "Full Journal Name=Abbr. Journal Name"
     *
     * @param in
     */
    private void readJournalList(Reader in) {
        BufferedReader reader = new BufferedReader(in);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                addLine(line);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException ex2) {
                ex2.printStackTrace();
            }
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
            if (fullName.length() <= 0 || abbrName.length() <= 0) {
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
