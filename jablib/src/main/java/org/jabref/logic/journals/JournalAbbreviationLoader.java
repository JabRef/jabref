package org.jabref.logic.journals;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jabref.logic.journals.ltwa.LtwaRepository;
import org.jabref.logic.util.TaskExecutor;

import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///
/// This class loads abbreviations from a CSV file and stores them into a MV file ([#readAbbreviationsFromCsvFile(Path)]).
/// It can also create an [JournalAbbreviationRepository] based on an MV file ([#loadRepository(AbbreviationPreferences)]).
///
///
/// Abbreviations are available at <a href="https://github.com/JabRef/abbrv.jabref.org/">https://github.com/JabRef/abbrv.jabref.org/</a>.
///
public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);
    private static final LtwaRepositoryCache LTWA_REPOSITORY = new LtwaRepositoryCache(Unchecked.supplier(JournalAbbreviationLoader::readLtwaRepository));

    /// LTWA trees are populated only during construction, and lookups keep parser state local.
    /// Cache successful initialization so journal-list reloads share this read-only bundled data.
    @NullMarked
    static final class LtwaRepositoryCache {
        private final Supplier<LtwaRepository> loader;
        private Optional<LtwaRepository> repository = Optional.empty();

        LtwaRepositoryCache(Supplier<LtwaRepository> loader) {
            this.loader = loader;
        }

        // [impl->req~journals.reuse-ltwa~1]
        // [impl->adr~share-bundled-ltwa~1]
        synchronized LtwaRepository get() {
            if (repository.isEmpty()) {
                repository = Optional.of(loader.get());
            }
            return repository.orElseThrow();
        }
    }

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
        return new AsyncJournalAbbreviationRepository(abbreviationPreferences, taskExecutor);
    }

    private static JournalAbbreviationRepository loadRepositorySynchronously(AbbreviationPreferences abbreviationPreferences) {
        JournalAbbreviationRepository repository;

        if (!abbreviationPreferences.shouldUseBuiltInJournalList()) {
            try {
                repository = new JournalAbbreviationRepository(loadLtwaRepository());
            } catch (UncheckedIOException e) {
                LOGGER.error("Error while loading LTWA repository", e);
                repository = new JournalAbbreviationRepository(new LtwaRepository());
            }
        } else {
            // Initialize with built-in list
            try (InputStream resourceAsStream = JournalAbbreviationRepository.class.getResourceAsStream("/journals/journal-list.mv")) {
                if (resourceAsStream == null) {
                    LOGGER.warn("There is no journal-list.mv. We use a default journal list.");
                    repository = new JournalAbbreviationRepository();
                } else {
                    Path tempDir = Files.createTempDirectory("jabref-journal");
                    Path tempJournalList = tempDir.resolve("journal-list.mv");
                    try {
                        Files.copy(resourceAsStream, tempJournalList);
                        repository = new JournalAbbreviationRepository(tempJournalList, loadLtwaRepository());
                    } finally {
                        Files.deleteIfExists(tempJournalList);
                        Files.deleteIfExists(tempDir);
                    }
                }
            } catch (IOException | UncheckedIOException e) {
                LOGGER.error("Error while loading journal abbreviation repository", e);
                return getFallbackRepository();
            }
        }

        // Read external lists
        // [impl->req~journals.disable-external~1]
        List<String> lists = new ArrayList<>(abbreviationPreferences.getExternalJournalLists().stream()
                                                                    .filter(path -> !abbreviationPreferences.getDisabledExternalJournalLists().contains(path))
                                                                    .toList());
        if (!lists.isEmpty()) {
            // reversing ensures that the latest lists overwrites the former one
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    repository.addLoadedAbbreviations(readAbbreviationsFromCsvFile(Path.of(filename)));
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

    static LtwaRepository loadLtwaRepository() {
        return LTWA_REPOSITORY.get();
    }

    private static LtwaRepository readLtwaRepository() throws IOException {
        try (InputStream resourceAsStream = JournalAbbreviationRepository.class.getResourceAsStream("/journals/ltwa-list.mv")) {
            if (resourceAsStream == null) {
                LOGGER.warn("There is no ltwa-list.mv. We cannot load the LTWA repository.");
                throw new IOException("LTWA repository not found");
            } else {
                Path tempDir = Files.createTempDirectory("jabref-ltwa");
                Path tempLtwaList = tempDir.resolve("ltwa-list.mv");
                try {
                    Files.copy(resourceAsStream, tempLtwaList);
                    return new LtwaRepository(tempLtwaList);
                } finally {
                    Files.deleteIfExists(tempLtwaList);
                    Files.deleteIfExists(tempDir);
                }
            }
        }
    }

    public static JournalAbbreviationRepository loadBuiltInRepository() {
        return loadRepository(new AbbreviationPreferences(List.of(), true, false));
    }

    private static class AsyncJournalAbbreviationRepository extends JournalAbbreviationRepository {
        private final AbbreviationPreferences abbreviationPreferences;
        private final TaskExecutor taskExecutor;
        private final AtomicBoolean loadingStarted = new AtomicBoolean(false);
        private final AtomicBoolean loaded = new AtomicBoolean(false);
        private final AtomicReference<JournalAbbreviationRepository> currentRepository = new AtomicReference<>();
        private final Set<Abbreviation> pendingCustomAbbreviations = new TreeSet<>();

        private AsyncJournalAbbreviationRepository(AbbreviationPreferences abbreviationPreferences, TaskExecutor taskExecutor) {
            this.abbreviationPreferences = abbreviationPreferences;
            this.taskExecutor = taskExecutor;
        }

        @Override
        public boolean isKnownName(String journalName) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .map(repository -> repository.isKnownName(journalName))
                    .orElse(false);
        }

        @Override
        public Optional<String> getLtwaAbbreviation(String journalName) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .flatMap(repository -> repository.getLtwaAbbreviation(journalName));
        }

        @Override
        public boolean isAbbreviatedName(String journalName) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .map(repository -> repository.isAbbreviatedName(journalName))
                    .orElse(false);
        }

        @Override
        public Optional<Abbreviation> get(String input) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .flatMap(repository -> repository.get(input));
        }

        @Override
        public void addCustomAbbreviation(@NonNull Abbreviation abbreviation) {
            ensureLoadingStarted();
            synchronized (pendingCustomAbbreviations) {
                pendingCustomAbbreviations.add(abbreviation);
                loadedDelegate().ifPresent(repository -> repository.addCustomAbbreviation(abbreviation));
            }
        }

        @Override
        public Collection<Abbreviation> getCustomAbbreviations() {
            ensureLoadingStarted();
            synchronized (pendingCustomAbbreviations) {
                return loadedDelegate()
                        .<Collection<Abbreviation>>map(repository -> Set.copyOf(repository.getCustomAbbreviations()))
                        .orElseGet(() -> Set.copyOf(pendingCustomAbbreviations));
            }
        }

        @Override
        public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd) {
            abbreviationsToAdd.forEach(this::addCustomAbbreviation);
        }

        @Override
        public Optional<String> getNextAbbreviation(String text) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .flatMap(repository -> repository.getNextAbbreviation(text));
        }

        @Override
        public Optional<String> getDefaultAbbreviation(String text) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .flatMap(repository -> repository.getDefaultAbbreviation(text));
        }

        @Override
        public Optional<String> getDotless(String text) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .flatMap(repository -> repository.getDotless(text));
        }

        @Override
        public Optional<String> getShortestUniqueAbbreviation(String text) {
            ensureLoadingStarted();
            return loadedDelegate()
                    .flatMap(repository -> repository.getShortestUniqueAbbreviation(text));
        }

        @Override
        public Set<String> getFullNames() {
            ensureLoadingStarted();
            return loadedDelegate()
                    .map(repository -> Set.copyOf(repository.getFullNames()))
                    .orElse(Set.of());
        }

        @Override
        public Collection<Abbreviation> getAllLoaded() {
            ensureLoadingStarted();
            return loadedDelegate()
                    .map(repository -> List.copyOf(repository.getAllLoaded()))
                    .orElse(List.of());
        }

        private void ensureLoadingStarted() {
            if (!loadingStarted.compareAndSet(false, true)) {
                return;
            }

            reload(abbreviationPreferences, taskExecutor);
        }

        @Override
        public synchronized void reload(AbbreviationPreferences preferences, TaskExecutor executor) {
            loadingStarted.set(true);
            super.reload(preferences, executor);
        }

        @Override
        protected void handleLoadFailure(long generation, AbbreviationPreferences preferences) {
            if (!loaded.get()) {
                JournalAbbreviationRepository fallback = preferences.shouldUseBuiltInJournalList()
                                                         ? new JournalAbbreviationRepository()
                                                         : new JournalAbbreviationRepository(new LtwaRepository());
                finishReload(generation, fallback);
            }
        }

        @Override
        protected void installRepository(JournalAbbreviationRepository repository) {
            synchronized (pendingCustomAbbreviations) {
                repository.addCustomAbbreviations(pendingCustomAbbreviations);
                currentRepository.set(repository);
                loaded.set(true);
            }
        }

        private Optional<JournalAbbreviationRepository> loadedDelegate() {
            if (!loaded.get()) {
                return Optional.empty();
            }
            return Optional.ofNullable(currentRepository.get());
        }
    }
}
