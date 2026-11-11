package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.journals.ltwa.LtwaRepository;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringSimilarity;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A repository for all journal abbreviations, including add and find methods.
public class JournalAbbreviationRepository {
    static final Pattern QUESTION_MARK = Pattern.compile("\\?");

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationRepository.class);
    private final StringSimilarity similarity = new StringSimilarity();
    private volatile RepositoryData data;
    private long loadGeneration;
    private final AtomicLong saveGeneration = new AtomicLong();
    private final Object saveLock = new Object();
    private final Map<Path, List<Abbreviation>> pendingJournalWrites = new HashMap<>();

    @NullMarked
    private static class RepositoryData {
        private final Map<String, Abbreviation> fullToAbbreviationObject = new HashMap<>();
        private final Map<String, Abbreviation> abbreviationToAbbreviationObject = new HashMap<>();
        private final Map<String, Abbreviation> dotlessToAbbreviationObject = new HashMap<>();
        private final Map<String, Abbreviation> shortestUniqueToAbbreviationObject = new HashMap<>();
        private final TreeSet<Abbreviation> customAbbreviations = new TreeSet<>();
        private final LtwaRepository ltwaRepository;

        private RepositoryData(LtwaRepository ltwaRepository) {
            this.ltwaRepository = ltwaRepository;
        }
    }

    public JournalAbbreviationRepository(LtwaRepository ltwaRepository) {
        this.data = new RepositoryData(ltwaRepository);
    }

    /// Initializes the internal data based on the abbreviations found in the given MV file
    ///
    /// @param journalList    The path to the MV file containing the journal abbreviations.
    /// @param ltwaRepository The LTWA repository to use for abbreviations.
    public JournalAbbreviationRepository(Path journalList, LtwaRepository ltwaRepository) {
        this(ltwaRepository);
        MVMap<String, Abbreviation> mvFullToAbbreviationObject;
        try (MVStore store = new MVStore.Builder().readOnly().fileName(journalList.toAbsolutePath().toString()).open()) {
            mvFullToAbbreviationObject = store.openMap("FullToAbbreviation");
            mvFullToAbbreviationObject.forEach((name, abbreviation) -> {
                String abbreviationString = abbreviation.getAbbreviation();
                String shortestUniqueAbbreviation = abbreviation.getShortestUniqueAbbreviation();
                Abbreviation newAbbreviation = new Abbreviation(
                        name,
                        abbreviationString,
                        shortestUniqueAbbreviation
                );
                data.fullToAbbreviationObject.put(name, newAbbreviation);
                data.abbreviationToAbbreviationObject.put(abbreviationString, newAbbreviation);
                data.dotlessToAbbreviationObject.put(newAbbreviation.getDotlessAbbreviation(), newAbbreviation);
                data.shortestUniqueToAbbreviationObject.put(shortestUniqueAbbreviation, newAbbreviation);
            });
        }
    }

    /// Initializes the repository with demonstration data. Used if no abbreviation file is found.
    public JournalAbbreviationRepository() {
        this(new LtwaRepository());
        Abbreviation newAbbreviation = new Abbreviation(
                "Demonstration",
                "Demo",
                "Dem"
        );
        data.fullToAbbreviationObject.put("Demonstration", newAbbreviation);
        data.abbreviationToAbbreviationObject.put("Demo", newAbbreviation);
        data.dotlessToAbbreviationObject.put("Demo", newAbbreviation);
        data.shortestUniqueToAbbreviationObject.put("Dem", newAbbreviation);
    }

    /// Loads a snapshot of saved preferences and publishes only the latest requested load.
    public synchronized void reload(AbbreviationPreferences preferences, TaskExecutor executor) {
        long generation = ++loadGeneration;
        AbbreviationPreferences snapshot = new AbbreviationPreferences(
                List.copyOf(preferences.getExternalJournalLists()), preferences.shouldUseFJournalField(), preferences.shouldEnableMscKeywordDescriptions());
        snapshot.setUseBuiltInJournalList(preferences.shouldUseBuiltInJournalList());
        BackgroundTask.wrap(() -> JournalAbbreviationLoader.loadRepository(snapshot))
                      .onSuccess(repository -> finishReload(generation, repository))
                      .onFailure(exception -> LOGGER.error("Failed to reload journal abbreviations", exception))
                      .executeWith(executor);
    }

    public void saveJournalLists(Map<Path, List<Abbreviation>> files, AbbreviationPreferences preferences,
                                 TaskExecutor executor, Runnable onSuccess) {
        long generation;
        Map<Path, List<Abbreviation>> pendingSnapshot;
        synchronized (pendingJournalWrites) {
            generation = saveGeneration.incrementAndGet();
            pendingJournalWrites.putAll(files);
            pendingSnapshot = Map.copyOf(pendingJournalWrites);
        }
        BackgroundTask.wrap(() -> {
                          synchronized (saveLock) {
                              if (generation == saveGeneration.get()) {
                                  for (Map.Entry<Path, List<Abbreviation>> file : pendingSnapshot.entrySet()) {
                                      AbbreviationWriter.writeOrCreate(file.getKey(), file.getValue());
                                  }
                              }
                          }
                          return true;
                      }).onSuccess(_ -> {
                          if (generation == saveGeneration.get()) {
                              synchronized (pendingJournalWrites) {
                                  if (generation == saveGeneration.get()) {
                                      pendingJournalWrites.clear();
                                  }
                              }
                              onSuccess.run();
                              reload(preferences, executor);
                          }
                      }).onFailure(exception -> LOGGER.error("Failed to store journal preferences", exception))
                      .executeWith(executor);
    }

    public Optional<List<Abbreviation>> getPendingJournalList(Path path) {
        synchronized (pendingJournalWrites) {
            return Optional.ofNullable(pendingJournalWrites.get(path));
        }
    }

    private synchronized void finishReload(long generation, JournalAbbreviationRepository repository) {
        if (generation == loadGeneration) {
            installRepository(repository);
        }
    }

    protected void installRepository(JournalAbbreviationRepository repository) {
        data = repository.data;
    }

    private static boolean isMatched(String name, Abbreviation abbreviation) {
        return name.equalsIgnoreCase(abbreviation.getName())
                || name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getDotlessAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    private static boolean isMatchedAbbreviated(String name, Abbreviation abbreviation) {
        boolean isExpanded = name.equalsIgnoreCase(abbreviation.getName());
        if (isExpanded) {
            return false;
        }
        return name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getDotlessAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    /// Returns true if the given journal name is contained in the list either in its full form
    /// (e.g., Physical Review Letters) or its abbreviated form (e.g., Phys. Rev. Lett.).
    /// If the exact match is not found, attempts a fuzzy match to recognize minor input errors.
    public boolean isKnownName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        return get(journalName).isPresent();
    }

    /// Get the LTWA abbreviation for the given journal name.
    public Optional<String> getLtwaAbbreviation(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return Optional.of(journalName);
        }
        return data.ltwaRepository.abbreviate(journalName);
    }

    /// Returns true if the given journal name is in its abbreviated form (e.g. Phys. Rev. Lett.). The test is strict,
    /// i.e., journals whose abbreviation is the same as the full name are not considered
    public boolean isAbbreviatedName(String journalName) {
        RepositoryData data = this.data;
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");
        return data.customAbbreviations.stream().anyMatch(abbreviation -> isMatchedAbbreviated(journal, abbreviation))
                || data.abbreviationToAbbreviationObject.containsKey(journal)
                || data.dotlessToAbbreviationObject.containsKey(journal)
                || data.shortestUniqueToAbbreviationObject.containsKey(journal);
    }

    /// Attempts to get the abbreviation of the journal given.
    /// if no exact match is found, attempts a fuzzy match on full journal names.
    ///
    /// @param input The journal name (either full name or abbreviated name).
    public Optional<Abbreviation> get(String input) {
        RepositoryData data = this.data;
        // Clean up input: trim and unescape ampersand
        String journal = input.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        Optional<Abbreviation> customAbbreviation = data.customAbbreviations.stream()
                                                                            .filter(abbreviation -> isMatched(journal, abbreviation))
                                                                            .findFirst();
        if (customAbbreviation.isPresent()) {
            return customAbbreviation;
        }

        Optional<Abbreviation> abbreviation = Optional.ofNullable(data.fullToAbbreviationObject.get(journal))
                                                      .or(() -> Optional.ofNullable(data.abbreviationToAbbreviationObject.get(journal)))
                                                      .or(() -> Optional.ofNullable(data.dotlessToAbbreviationObject.get(journal)))
                                                      .or(() -> Optional.ofNullable(data.shortestUniqueToAbbreviationObject.get(journal)));

        if (abbreviation.isEmpty()) {
            abbreviation = findAbbreviationFuzzyMatched(data, journal);
        }

        return abbreviation;
    }

    private Optional<Abbreviation> findAbbreviationFuzzyMatched(RepositoryData data, String input) {
        Optional<Abbreviation> customMatch = findBestFuzzyMatched(data.customAbbreviations, input);
        if (customMatch.isPresent()) {
            return customMatch;
        }

        return findBestFuzzyMatched(data.fullToAbbreviationObject.values(), input);
    }

    /// Fuzzy searches abbreviations
    ///
    /// To prevent wrong matches, such as Nutrients converted to Nutrition we do fuzzy matching if the title is three words or more (or contains non-ASCII characters)
    private Optional<Abbreviation> findBestFuzzyMatched(Collection<Abbreviation> abbreviations, String input) {
        // Only one or two words - do not do fuzzy matching
        if (input.trim().split("\\s+").length <= 1) {
            // Some Chinese titles do not contain spaces
            boolean isAscii = input.chars().allMatch(c -> c <= 0x7F);
            if (isAscii) {
                return Optional.empty();
            }
        }

        // threshold for edit distance similarity comparison
        final double SIMILARITY_THRESHOLD = 1.0;

        List<Abbreviation> candidates = abbreviations.stream()
                                                     .filter(abbreviation -> similarity.isSimilar(input, abbreviation.getName()))
                                                     .sorted(Comparator.comparingDouble(abbreviation -> similarity.editDistanceIgnoreCase(input, abbreviation.getName())))
                                                     .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        if (candidates.size() > 1) {
            double bestDistance = similarity.editDistanceIgnoreCase(input, candidates.getFirst().getName());
            double secondDistance = similarity.editDistanceIgnoreCase(input, candidates.get(1).getName());

            // If there is a very close match of two abbreviations, do not use any of them, because they are too close.
            if (Math.abs(bestDistance - secondDistance) < SIMILARITY_THRESHOLD) {
                return Optional.empty();
            }
        }

        return Optional.of(candidates.getFirst());
    }

    public void addCustomAbbreviation(@NonNull Abbreviation abbreviation) {
        // We do NOT want to keep duplicates
        // The set automatically "removes" duplicates
        // What is a duplicate? An abbreviation is NOT the same if any field is NOT equal (e.g., if the shortest unique differs, the abbreviation is NOT the same)
        data.customAbbreviations.add(abbreviation);
    }

    public Collection<Abbreviation> getCustomAbbreviations() {
        return data.customAbbreviations;
    }

    public void addCustomAbbreviations(Collection<Abbreviation> abbreviationsToAdd) {
        abbreviationsToAdd.forEach(this::addCustomAbbreviation);
    }

    public Optional<String> getNextAbbreviation(String text) {
        return get(text).map(abbreviation -> abbreviation.getNext(text));
    }

    public Optional<String> getDefaultAbbreviation(String text) {
        return get(text).map(Abbreviation::getAbbreviation);
    }

    public Optional<String> getDotless(String text) {
        return get(text).map(Abbreviation::getDotlessAbbreviation);
    }

    public Optional<String> getShortestUniqueAbbreviation(String text) {
        return get(text).map(Abbreviation::getShortestUniqueAbbreviation);
    }

    public Set<String> getFullNames() {
        return data.fullToAbbreviationObject.keySet();
    }

    public Collection<Abbreviation> getAllLoaded() {
        return data.fullToAbbreviationObject.values();
    }
}
