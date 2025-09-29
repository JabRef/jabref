package org.jabref.logic.protectedterms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NonNull;
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

    public void readTermsFromResource(@NonNull String resourceFileName, String descriptionString) {
        description = descriptionString;
        location = resourceFileName;
        try (InputStream inputStream = ProtectedTermsLoader.class.getResourceAsStream(resourceFileName)) {
            if (inputStream == null) {
                LOGGER.error("Cannot find resource '{}' ({})", resourceFileName, descriptionString);
                return;
            }
            readTermsList(inputStream);
        } catch (IOException e) {
            LOGGER.error("Cannot open resource '{}'", resourceFileName, e);
        }
    }

    public void readTermsFromFile(Path path) {
        location = path.toString();

        path = path.toAbsolutePath();
        if (!Files.exists(path)) {
            LOGGER.warn("Could not read terms from file {}", path);
            return;
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            readTermsList(inputStream);
        } catch (IOException e) {
            LOGGER.error("Cannot open file '{}'", path, e);
        }
    }

    private void readTermsList(InputStream inputStream) {
        try (Stream<String> lines = new BufferedReader(new InputStreamReader(inputStream)).lines()) {
            this.terms.addAll(lines.map(this::setDescription).filter(Objects::nonNull).toList());
        } catch (UncheckedIOException e) {
            LOGGER.warn("Could not read terms from stream", e);
        }
    }

    /**
     * Parse the description that starts after the # but don't include it in the terms
     *
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
