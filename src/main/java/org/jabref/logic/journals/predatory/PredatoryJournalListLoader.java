package org.jabref.logic.journals.predatory;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredatoryJournalListLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PredatoryJournalListLoader.class);

    public static PredatoryJournalRepository loadRepository() {
        PredatoryJournalRepository repository = new PredatoryJournalRepository();

        Path path;
        try {
            URL resource = PredatoryJournalRepository.class.getResource("/journals/predatory-journals.mv");
            if (resource == null) {
                LOGGER.error("predatoryJournal-list.mv not found. Using demo list.");
                return new PredatoryJournalRepository();
            }
            path = Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            LOGGER.error("Could not determine path to predatoryJournal-list.mv. Using demo list.");
            return new PredatoryJournalRepository();
        }

        return new PredatoryJournalRepository(path);
    }
}
