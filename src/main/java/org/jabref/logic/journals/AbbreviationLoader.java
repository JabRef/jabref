package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbbreviationLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);
    private String mvName;

    public AbbreviationLoader(String path) {
        this.mvName = path;
    }

    public List<Abbreviation> readListFromFile(Path file) throws IOException {
        LOGGER.debug(String.format("Reading journal list from file %s", file));
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    public AbbreviationRepository loadRepository(AbbreviationPreferences abbreviationPreferences) {
        AbbreviationRepository repository;
        // Initialize with built-in list
        try {
            Path tempDir = Files.createTempDirectory("jabref-abbreviation-loading");
            Path tempList = tempDir.resolve(mvName);
            Files.copy(AbbreviationRepository.class.getResourceAsStream("resources/" + mvName), tempList);
            repository = new AbbreviationRepository(tempList);
            tempDir.toFile().deleteOnExit();
            tempList.toFile().deleteOnExit();
        } catch (IOException e) {
            LOGGER.error("Error while copying list", e);
            return null;
        }

        // Read external lists
        List<String> lists = abbreviationPreferences.getExternalLists();
        if (!(lists.isEmpty())) {
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    repository.addCustomAbbreviations(readListFromFile(Path.of(filename)));
                } catch (IOException e) {
                    LOGGER.error(String.format("Cannot read external list file %s", filename), e);
                }
            }
        }
        return repository;
    }
}
