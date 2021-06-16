package org.jabref.logic.protectedterms;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads abbreviation files (property files using NAME = ABBREVIATION as a format) into a list of Abbreviations.
 */
public class ProtectedTermsParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedTermsParser.class);

    private final List<String> terms = new ArrayList<>();
    private String description = Localization.lang("The text after the last line starting with # will be used");

    private String location;

    public void readTermsFromResource(String resourceFileName, String descriptionString) {
        URL url = Objects
                .requireNonNull(ProtectedTermsLoader.class.getResource(Objects.requireNonNull(resourceFileName)));
        description = descriptionString;
        location = resourceFileName;
        try {
            readTermsList(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.info("Could not read protected terms from resource " + resourceFileName, e);
        }
    }

    public void readTermsFromFile(File file) throws FileNotFoundException {
        location = file.getAbsolutePath();
        try (FileReader reader = new FileReader(Objects.requireNonNull(file))) {
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

    public ProtectedTermsList getProtectTermsList(boolean enabled, boolean internal) {
        ProtectedTermsList termList = new ProtectedTermsList(description, terms, location, internal);
        termList.setEnabled(enabled);
        return termList;
    }
}
