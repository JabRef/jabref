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
package net.sf.jabref.logic.protectterms;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Reads abbreviation files (property files using NAME = ABBREVIATION as a format) into a list of Abbreviations.
 */
public class ProtectTermsParser {

    private final List<String> terms = new ArrayList<>();

    private String description = Localization.lang("The text after the last line starting with # will be used");
    private String location;

    private static final Log LOGGER = LogFactory.getLog(ProtectTermsParser.class);


    public void readTermsFromFile(File file) throws FileNotFoundException {
        location = file.getAbsolutePath();
        try(FileReader reader = new FileReader(Objects.requireNonNull(file))) {
            readTermsList(reader);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn("Could not read terms from file " + file.getAbsolutePath(), e);
        }
    }

    public void readTermsFromFile(File file, Charset encoding) throws FileNotFoundException {
        location = file.getAbsolutePath();
        try (FileInputStream stream = new FileInputStream(Objects.requireNonNull(file));
                InputStreamReader reader = new InputStreamReader(stream, Objects.requireNonNull(encoding))) {
            readTermsList(reader);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            LOGGER.warn("Could not read terms from file " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Read the given file, which should contain a list of journal names and their
     * abbreviations. Each line should be formatted as: "Full Journal Name=Abbr. Journal Name"
     *
     * @param in
     */
    private void readTermsList(Reader in) {
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
            description = line.substring(1).trim();
            return;
        }
        this.terms.add(line);
    }

    public ProtectTermsList getProtectTermsList() {
        return new ProtectTermsList(description, terms, location);
    }
}
