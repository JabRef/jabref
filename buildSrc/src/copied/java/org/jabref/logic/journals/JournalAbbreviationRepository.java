package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {
    static final Pattern QUESTION_MARK = Pattern.compile("\\?");

    private final Map<String, Abbreviation> fullToAbbreviationObject = new HashMap<>();
    private final Map<String, Abbreviation> abbreviationToAbbreviationObject = new HashMap<>();
    private final Map<String, Abbreviation> dotlessToAbbreviationObject = new HashMap<>();
    private final Map<String, Abbreviation> shortestUniqueToAbbreviationObject = new HashMap<>();
    private final List<Abbreviation> customAbbreviations = new ArrayList<>();

    public JournalAbbreviationRepository(Path journalList) {
        MVStore store = new MVStore.Builder().readOnly().fileName(journalList.toAbsolutePath().toString()).open();
        MVMap<String, Abbreviation> mvFullToAbbreviationObject = store.openMap("FullToAbbreviation");

        mvFullToAbbreviationObject.forEach((name, abbreviation) -> {
            String abbrevationString = abbreviation.getAbbreviation();
            String shortestUniqueAbbreviation = abbreviation.getShortestUniqueAbbreviation();
            Abbreviation newAbbreviation = new Abbreviation(
                    name,
                    abbrevationString,
                    shortestUniqueAbbreviation
            );
            fullToAbbreviationObject.put(name, newAbbreviation);
            abbreviationToAbbreviationObject.put(abbrevationString, newAbbreviation);
            dotlessToAbbreviationObject.put(newAbbreviation.getDotlessAbbreviation(), newAbbreviation);
            shortestUniqueToAbbreviationObject.put(shortestUniqueAbbreviation, newAbbreviation);
        });
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
        boolean isAbbreviated = name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getDotlessAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
        return isAbbreviated;
    }

    /**
     * Returns true if the given journal name is contained in the list either in its full form
     * (e.g., Physical Review Letters) or its abbreviated form (e.g., Phys. Rev. Lett.).
     */
    public boolean isKnownName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");
        return customAbbreviations.stream().anyMatch(abbreviation -> isMatched(journal, abbreviation))
                || fullToAbbreviationObject.containsKey(journal)
                || abbreviationToAbbreviationObject.containsKey(journal)
                || dotlessToAbbreviationObject.containsKey(journal)
                || shortestUniqueToAbbreviationObject.containsKey(journal);
    }

    /**
     * Returns true if the given journal name is in its abbreviated form (e.g. Phys. Rev. Lett.). The test is strict,
     * i.e., journals whose abbreviation is the same as the full name are not considered
     */
    public boolean isAbbreviatedName(String journalName) {
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }
        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");
        return customAbbreviations.stream().anyMatch(abbreviation -> isMatchedAbbreviated(journal, abbreviation))
                || abbreviationToAbbreviationObject.containsKey(journal)
                || dotlessToAbbreviationObject.containsKey(journal)
                || shortestUniqueToAbbreviationObject.containsKey(journal);
    }

    /**
     * Attempts to get the abbreviation of the journal given.
     *
     * @param input The journal name (either full name or abbreviated name).
     */
    public Optional<Abbreviation> get(String input) {
        // Clean up input: trim and unescape ampersand
        String journal = input.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        Optional<Abbreviation> customAbbreviation = customAbbreviations.stream()
                                                                       .filter(abbreviation -> isMatched(journal, abbreviation))
                                                                       .findAny();
        if (customAbbreviation.isPresent()) {
            return customAbbreviation;
        }

        return Optional.ofNullable(fullToAbbreviationObject.get(journal))
                .or(() -> Optional.ofNullable(abbreviationToAbbreviationObject.get(journal)))
                .or(() -> Optional.ofNullable(dotlessToAbbreviationObject.get(journal)))
                .or(() -> Optional.ofNullable(shortestUniqueToAbbreviationObject.get(journal)));
    }

    public void addCustomAbbreviation(Abbreviation abbreviation) {
        Objects.requireNonNull(abbreviation);

        // We do not want to keep duplicates, thus remove the old abbreviation
        // (abbreviation equality is tested on name only, so we cannot use a Set instead)
        customAbbreviations.remove(abbreviation);
        customAbbreviations.add(abbreviation);
    }

    public List<Abbreviation> getCustomAbbreviations() {
        return customAbbreviations;
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
        return fullToAbbreviationObject.keySet();
    }

    public Collection<Abbreviation> getAllLoaded() {
        return fullToAbbreviationObject.values();
    }
}
