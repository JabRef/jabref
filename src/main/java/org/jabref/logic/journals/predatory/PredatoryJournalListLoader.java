package org.jabref.logic.journals.predatory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredatoryJournalListLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalListLoader.class);

    public static PredatoryJournalRepository loadRepository() {
        PredatoryJournalRepository repository = new PredatoryJournalRepository();
        // Initialize with built-in list
        // We cannot use PredatoryJournalRepository.class.getResource, because this is null in JPackage, thus we need "getResourceAsStream"
        try (InputStream resourceAsStream = PredatoryJournalListLoader.class.getResourceAsStream("/journals/predatory-journals.mv")) {
            if (resourceAsStream == null) {
                LOGGER.warn("There is no predatory-journal.mv. We use a default predatory dummy list");
                repository = new PredatoryJournalRepository();
            } else {
                // MVStore does not support loading from stream. Thus, we need to have a file copy of the stream.
                Path tempDir = Files.createTempDirectory("jabref-journal");
                Path tempJournalList = tempDir.resolve("predatory-journals.mv");
                Files.copy(resourceAsStream, tempJournalList);
                repository = new PredatoryJournalRepository(tempJournalList);
                tempDir.toFile().deleteOnExit();
                tempJournalList.toFile().deleteOnExit();
            }
        } catch (IOException e) {
            LOGGER.error("Error while copying journal list", e);
        }
        return repository;
    }
}
