package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.journals.ltwa.LtwaRepository;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class JournalAbbreviationLoader extends AbbreviationLoader<JournalAbbreviationRepository> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    public JournalAbbreviationLoader() {
        super("journals", "journal-list.mv");
    }

    public static JournalAbbreviationRepository loadRepository(JournalAbbreviationPreferences abbreviationPreferences) {
        return new JournalAbbreviationLoader().loadRepositorySynchronously(abbreviationPreferences);
    }

    public static JournalAbbreviationRepository loadRepositoryInBackground(JournalAbbreviationPreferences abbreviationPreferences, TaskExecutor taskExecutor) {
        return new AsyncJournalAbbreviationRepository(abbreviationPreferences, taskExecutor);
    }

    @Override
    protected JournalAbbreviationRepository createRepository(Path file) throws IOException {
        return new JournalAbbreviationRepository(file, loadLtwaRepository());
    }

    @Override
    protected JournalAbbreviationRepository createFallbackRepository() {
        return getFallbackRepository();
    }

    private static JournalAbbreviationRepository getFallbackRepository() {
        LOGGER.warn("Falling back to the default journal abbreviation repository");
        return new JournalAbbreviationRepository();
    }

    private static LtwaRepository loadLtwaRepository() throws IOException {
        Path file = extractResource("/journals/ltwa-list.mv")
                .orElseThrow(() -> new IOException("LTWA repository not found"));
        return new LtwaRepository(file);
    }

    public static JournalAbbreviationRepository loadBuiltInRepository() {
        return loadRepository(JournalAbbreviationPreferences.getDefault());
    }

    @NullMarked
    private static class AsyncJournalAbbreviationRepository extends JournalAbbreviationRepository {
        private final JournalAbbreviationPreferences abbreviationPreferences;
        private final TaskExecutor taskExecutor;
        private final AtomicBoolean loadingStarted = new AtomicBoolean(false);
        private final AtomicBoolean loaded = new AtomicBoolean(false);
        private final AtomicReference<JournalAbbreviationRepository> currentRepository = new AtomicReference<>();
        private final Set<Abbreviation> pendingCustomAbbreviations = new TreeSet<>();

        private AsyncJournalAbbreviationRepository(JournalAbbreviationPreferences abbreviationPreferences, TaskExecutor taskExecutor) {
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

            BackgroundTask.wrap(() -> loadRepository(abbreviationPreferences))
                          .onSuccess(this::setLoadedRepository)
                          .onFailure(e -> {
                              LOGGER.error("Error while loading journal abbreviation repository in background", e);
                              setFallbackRepository();
                          })
                          .executeWith(taskExecutor);
        }

        private void setLoadedRepository(JournalAbbreviationRepository repository) {
            synchronized (pendingCustomAbbreviations) {
                repository.addCustomAbbreviations(pendingCustomAbbreviations);
                currentRepository.set(repository);
                loaded.set(true);
            }
        }

        private void setFallbackRepository() {
            synchronized (pendingCustomAbbreviations) {
                JournalAbbreviationRepository fallbackRepository = getFallbackRepository();
                fallbackRepository.addCustomAbbreviations(pendingCustomAbbreviations);
                currentRepository.set(fallbackRepository);
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
