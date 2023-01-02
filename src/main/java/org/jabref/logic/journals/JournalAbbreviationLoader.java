package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *   This class loads abbreviations from a CSV file and stores them into a MV file
 * </p>
 * <p>
 *   Abbreviations are available at <a href="https://github.com/JabRef/abbrv.jabref.org/">https://github.com/JabRef/abbrv.jabref.org/</a>.
 * </p>
 */
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
            Path tempJournalList = tempDir.resolve("journal-list.mv");
            Files.copy(JournalAbbreviationRepository.class.getResourceAsStream("/journals/journal-list.mv"), tempJournalList);
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
            // reversing ensures that the latest lists overwrites the former one
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    repository.addCustomAbbreviations(readJournalListFromFile(Path.of(filename)));
                } catch (IOException e) {
                    LOGGER.error("Cannot read external journal list file {}", filename, e);
                }
            }
        }
        return repository;
    }

    public static JournalAbbreviationRepository loadBuiltInRepository() {
        return loadRepository(new JournalAbbreviationPreferences(Collections.emptyList(), StandardCharsets.UTF_8, true));
    }
}
