package org.jabref.logic.protectedterms;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        try {
            Path path = Path.of(Objects.requireNonNull(ProtectedTermsLoader.class.getResource(Objects.requireNonNull(resourceFileName))).toURI());
            readTermsList(path);
            description = descriptionString;
            location = resourceFileName;
        } catch (URISyntaxException e1) {
            LOGGER.error("");
        }
    }

    public void readTermsFromFile(Path path) {
        location = path.toAbsolutePath().toString();
        readTermsList(path);
    }

    private void readTermsList(Path path) {
        if (!Files.exists(path)) {
            LOGGER.warn("Could not read terms from file {}", path);
            return;
        }
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            this.terms.addAll(lines.map(this::setDescription).filter(line -> line != null).collect(Collectors.toList()));
        } catch (IOException e) {
            LOGGER.warn("Could not read terms from file {}", path, e);
        }
    }

    /**
     * Parse the description that starts after the # but don't include it in the terms
     *
     * @param line
     * @return line or null if the line contains the description
     */
    private String setDescription(String line) {
        if (line.startsWith("#")) {
            description = line.substring(1).trim();
            return null;
        }
        return line;
    }

    public ProtectedTermsList getProtectTermsList(boolean enabled, boolean internal) {
        ProtectedTermsList termList = new ProtectedTermsList(description, terms, location, internal);
        termList.setEnabled(enabled);
        return termList;
    }
}
