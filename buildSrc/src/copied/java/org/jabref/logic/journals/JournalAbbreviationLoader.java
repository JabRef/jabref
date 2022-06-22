package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    public static List<Abbreviation> readJournalListFromFile(Path file) throws IOException {
        LOGGER.debug(String.format("Reading journal list from file %s", file));
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    public static JournalAbbreviationRepository loadRepository(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        JournalAbbreviationRepository repository;
        // Initialize with built-in list
        try {
            Path tempDir = Files.createTempDirectory("jabref-journal");
            Path tempJournalList = tempDir.resolve("journalList.mv");
            Files.copy(JournalAbbreviationRepository.class.getResourceAsStream("/journals/journalList.mv"), tempJournalList);
            repository = new JournalAbbreviationRepository(tempJournalList);
            tempDir.toFile().deleteOnExit();
            tempJournalList.toFile().deleteOnExit();
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
