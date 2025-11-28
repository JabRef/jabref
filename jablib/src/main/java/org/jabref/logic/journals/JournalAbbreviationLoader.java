package org.jabref.logic.journals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.journals.ltwa.LtwaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This class loads abbreviations from a CSV file and stores them into a MV file ({@link #readAbbreviationsFromCsvFile(Path)}
 * It can also create an {@link JournalAbbreviationRepository} based on an MV file ({@link #loadRepository(JournalAbbreviationPreferences)}.
 * </p>
 * <p>
 * Abbreviations are available at <a href="https://github.com/JabRef/abbrv.jabref.org/">https://github.com/JabRef/abbrv.jabref.org/</a>.
 * </p>
 */
public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    public static Collection<Abbreviation> readAbbreviationsFromCsvFile(Path file) throws IOException {
        LOGGER.debug("Reading journal list from file {}", file);
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    public static JournalAbbreviationRepository loadRepository(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        JournalAbbreviationRepository repository;

        // Initialize with built-in list
        try (InputStream resourceAsStream = JournalAbbreviationRepository.class.getResourceAsStream("/journals/journal-list.mv")) {
            if (resourceAsStream == null) {
                LOGGER.warn("There is no journal-list.mv. We use a default journal list.");
                repository = new JournalAbbreviationRepository();
            } else {
                Path tempDir = Files.createTempDirectory("jabref-journal");
                Path tempJournalList = tempDir.resolve("journal-list.mv");
                Files.copy(resourceAsStream, tempJournalList);
                repository = new JournalAbbreviationRepository(tempJournalList, loadLtwaRepository());
                tempDir.toFile().deleteOnExit();
                tempJournalList.toFile().deleteOnExit();
                LOGGER.debug("Loaded journal abbreviations from {}", tempJournalList.toAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.error("Error while loading journal abbreviation repository", e);
            return null;
        }

        // Read external lists
        List<String> lists = journalAbbreviationPreferences.getExternalJournalLists();
        // might produce NPE in tests
        if (lists != null && !lists.isEmpty()) {
            // reversing ensures that the latest lists overwrites the former one
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    repository.addCustomAbbreviations(readAbbreviationsFromCsvFile(Path.of(filename)));
                } catch (IOException | InvalidPathException e) {
                    // invalid path might come from unix/windows mixup of prefs
                    LOGGER.error("Cannot read external journal list file {}", filename, e);
                }
            }
        }
        return repository;
    }

    private static LtwaRepository loadLtwaRepository() throws IOException {
        try (InputStream resourceAsStream = JournalAbbreviationRepository.class.getResourceAsStream("/journals/ltwa-list.mv")) {
            if (resourceAsStream == null) {
                LOGGER.warn("There is no ltwa-list.mv. We cannot load the LTWA repository.");
                throw new IOException("LTWA repository not found");
            } else {
                Path tempDir = Files.createTempDirectory("jabref-ltwa");
                Path tempLtwaList = tempDir.resolve("ltwa-list.mv");
                Files.copy(resourceAsStream, tempLtwaList);
                LtwaRepository ltwaRepository = new LtwaRepository(tempLtwaList);
                tempDir.toFile().deleteOnExit();
                tempLtwaList.toFile().deleteOnExit();
                return ltwaRepository;
            }
        }
    }

    public static JournalAbbreviationRepository loadBuiltInRepository() {
        return loadRepository(new JournalAbbreviationPreferences(List.of(), true));
    }
}
