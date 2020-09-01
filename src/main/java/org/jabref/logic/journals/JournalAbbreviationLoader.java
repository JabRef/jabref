package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    private static final String JOURNALS_FILE_BUILTIN = "/journals/journalList.csv";

    public static List<Abbreviation> getBuiltInAbbreviations() {
        return readJournalListFromResource(JOURNALS_FILE_BUILTIN);
    }

    private static List<Abbreviation> readJournalListFromResource(String resource) {
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromResource(Objects.requireNonNull(resource));
        return parser.getAbbreviations();
    }

    public static List<Abbreviation> readJournalListFromFile(Path file) throws IOException {
        LOGGER.debug(String.format("Reading journal list from file %s", file));
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    private static List<Abbreviation> readJournalListFromFile(Path file, Charset encoding) throws IOException {
        LOGGER.debug(String.format("Reading journal list from file %s", file));
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file, Objects.requireNonNull(encoding));
        return parser.getAbbreviations();
    }

    public static JournalAbbreviationRepository loadRepository(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        JournalAbbreviationRepository repository;
        // Initialize with built-in list
        try {
            Path tempJournalList = Files.createTempDirectory("journal").resolve("journalList.mv");
            Files.copy(JournalAbbreviationRepository.class.getResourceAsStream("/journals/journalList.mv"), tempJournalList);
            repository = new JournalAbbreviationRepository(tempJournalList);
        } catch (IOException e) {
            LOGGER.error("Error while copying journal list", e);
            return null;
        }

        // Read external lists
        List<String> lists = journalAbbreviationPreferences.getExternalJournalLists();
        if (!(lists.isEmpty())) {
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    repository.addCustomAbbreviations(readJournalListFromFile(Path.of(filename)));
                } catch (IOException e) {
                    LOGGER.error(String.format("Cannot read external journal list file %s", filename), e);
                }
            }
        }
        return repository;
    }

    public static JournalAbbreviationRepository loadBuiltInRepository() {
        return loadRepository(new JournalAbbreviationPreferences(Collections.emptyList(), StandardCharsets.UTF_8));
    }
}
