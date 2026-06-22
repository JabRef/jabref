package org.jabref.logic.journals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.journals.ltwa.LtwaRepository;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///
/// This class loads abbreviations from a CSV file and stores them into a MV file ({@link #readAbbreviationsFromCsvFile(Path)}
/// It can also create an {@link JournalAbbreviationRepository} based on an MV file ({@link #loadRepository(AbbreviationPreferences)}.
///
///
/// Abbreviations are available at <a href="https://github.com/JabRef/abbrv.jabref.org/">https://github.com/JabRef/abbrv.jabref.org/</a>.
///
public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    public static Collection<Abbreviation> readAbbreviationsFromCsvFile(Path file) throws IOException {
        LOGGER.debug("Reading journal list from file {}", file);
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    public static JournalAbbreviationRepository loadRepository(AbbreviationPreferences abbreviationPreferences) {
        return loadRepositorySynchronously(abbreviationPreferences);
    }

    public static JournalAbbreviationRepository loadRepositoryInBackground(AbbreviationPreferences abbreviationPreferences, TaskExecutor taskExecutor) {
        AsyncJournalAbbreviationRepository repository = new AsyncJournalAbbreviationRepository();
        BackgroundTask.wrap(() -> loadRepositorySynchronously(abbreviationPreferences))
                      .onSuccess(repository::setLoadedRepository)
                      .onFailure(e -> {
                          LOGGER.error("Error while loading journal abbreviation repository in background", e);
                          repository.setFallbackRepository();
                      })
                      .executeWith(taskExecutor);
        return repository;
    }

    private static JournalAbbreviationRepository loadRepositorySynchronously(AbbreviationPreferences abbreviationPreferences) {
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
            return getFallbackRepository();
        }

        // Read external lists
        List<String> lists = abbreviationPreferences.getExternalJournalLists();
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

    private static JournalAbbreviationRepository getFallbackRepository() {
        LOGGER.warn("Falling back to the default journal abbreviation repository");
        return new JournalAbbreviationRepository();
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
        return loadRepository(new AbbreviationPreferences(List.of(), true, false));
    }

    private static class AsyncJournalAbbreviationRepository extends JournalAbbreviationRepository {
        private final AtomicBoolean loaded = new AtomicBoolean(false);
        private final AtomicReference<JournalAbbreviationRepository> currentRepository = new AtomicReference<>();
        private final Set<Abbreviation> pendingCustomAbbreviations = new TreeSet<>();

        private AsyncJournalAbbreviationRepository() {
            currentRepository.set(getFallbackRepository());
        }

        @Override
        public boolean isKnownName(String journalName) {
            return loadedDelegate()
                    .map(repository -> repository.isKnownName(journalName))
                    .orElse(false);
        }

        @Override
        public Optional<String> getLtwaAbbreviation(String journalName) {
            return loadedDelegate()
                    .flatMap(repository -> repository.getLtwaAbbreviation(journalName));
        }

        @Override
        public boolean isAbbreviatedName(String journalName) {
            return loadedDelegate()
                    .map(repository -> repository.isAbbreviatedName(journalName))
                    .orElse(false);
        }

        @Override
        public Optional<Abbreviation> get(String input) {
            return loadedDelegate()
                    .flatMap(repository -> repository.get(input));
        }

        @Override
        public void addCustomAbbreviation(@NonNull Abbreviation abbreviation) {
            synchronized (pendingCustomAbbreviations) {
                pendingCustomAbbreviations.add(abbreviation);
                currentRepository().addCustomAbbreviation(abbreviation);
            }
        }

        @Override
        public Collection<Abbreviation> getCustomAbbreviations() {
            return Set.copyOf(currentRepository().getCustomAbbreviations());
        }

        @Override
        public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd) {
            abbreviationsToAdd.forEach(this::addCustomAbbreviation);
        }

        @Override
        public Optional<String> getNextAbbreviation(String text) {
            return loadedDelegate()
                    .flatMap(repository -> repository.getNextAbbreviation(text));
        }

        @Override
        public Optional<String> getDefaultAbbreviation(String text) {
            return loadedDelegate()
                    .flatMap(repository -> repository.getDefaultAbbreviation(text));
        }

        @Override
        public Optional<String> getDotless(String text) {
            return loadedDelegate()
                    .flatMap(repository -> repository.getDotless(text));
        }

        @Override
        public Optional<String> getShortestUniqueAbbreviation(String text) {
            return loadedDelegate()
                    .flatMap(repository -> repository.getShortestUniqueAbbreviation(text));
        }

        @Override
        public Set<String> getFullNames() {
            return loadedDelegate()
                    .map(repository -> Set.copyOf(repository.getFullNames()))
                    .orElse(Set.of());
        }

        @Override
        public Collection<Abbreviation> getAllLoaded() {
            return loadedDelegate()
                    .map(repository -> List.copyOf(repository.getAllLoaded()))
                    .orElse(List.of());
        }

        private void setLoadedRepository(JournalAbbreviationRepository repository) {
            synchronized (pendingCustomAbbreviations) {
                repository.addCustomAbbreviations(pendingCustomAbbreviations);
                currentRepository.set(repository);
                loaded.set(true);
            }
        }

        private void setFallbackRepository() {
            currentRepository.set(getFallbackRepository());
            loaded.set(true);
        }

        private Optional<JournalAbbreviationRepository> loadedDelegate() {
            if (!loaded.get()) {
                return Optional.empty();
            }
            return Optional.of(currentRepository());
        }

        private JournalAbbreviationRepository currentRepository() {
            JournalAbbreviationRepository repository = currentRepository.get();
            if (repository == null) {
                JournalAbbreviationRepository fallbackRepository = getFallbackRepository();
                currentRepository.set(fallbackRepository);
                return fallbackRepository;
            }
            return repository;
        }
    }
}
