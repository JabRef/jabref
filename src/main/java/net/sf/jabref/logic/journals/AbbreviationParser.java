/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.journals;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Reads abbreviation files (property files using NAME = ABBREVIATION as a format) into a list of Abbreviations.
 */
public class AbbreviationParser {

    private final List<Abbreviation> abbreviations = new LinkedList<>();

    private static final Log LOGGER = LogFactory.getLog(AbbreviationParser.class);

    public void readJournalListFromResource(String resourceFileName) {
        URL url = Objects.requireNonNull(JournalAbbreviationRepository.class.getResource(Objects.requireNonNull(resourceFileName)));
        try {
            readJournalList(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.info("Could not read journal list from file " + resourceFileName, e);
        }
    }

    public void readJournalListFromFile(File file) throws FileNotFoundException {
        try(FileReader reader = new FileReader(Objects.requireNonNull(file))) {
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
        try(BufferedReader reader = new BufferedReader(in)){
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
