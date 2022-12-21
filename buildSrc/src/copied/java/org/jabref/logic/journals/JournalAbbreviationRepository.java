package org.jabref.logic.journals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

/**
 * A repository for all journal abbreviations, including add and find methods.
 */
public class JournalAbbreviationRepository {
    static final Pattern DOT = Pattern.compile("\\.");
    static final Pattern QUESTION_MARK = Pattern.compile("\\?");

    private final MVMap<String, String> fullToAbbreviation;
    private final MVMap<String, String> abbreviationToFull;
    private final List<Abbreviation> customAbbreviations;

    public JournalAbbreviationRepository(Path journalList) {
        MVStore store = new MVStore.Builder().readOnly().fileName(journalList.toAbsolutePath().toString()).open();
        this.fullToAbbreviation = store.openMap("FullToAbbreviation");
        this.abbreviationToFull = store.openMap("AbbreviationToFull");
        this.customAbbreviations = new ArrayList<>();
    }

    private static boolean isMatched(String name, Abbreviation abbreviation) {
        return name.equalsIgnoreCase(abbreviation.getName())
                || name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getMedlineAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
    }

    private static boolean isMatchedAbbreviated(String name, Abbreviation abbreviation) {
        boolean isAbbreviated = name.equalsIgnoreCase(abbreviation.getAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getMedlineAbbreviation())
                || name.equalsIgnoreCase(abbreviation.getShortestUniqueAbbreviation());
        boolean isExpanded = name.equalsIgnoreCase(abbreviation.getName());
        return isAbbreviated && !isExpanded;
    }

    /**
     * Returns true if the given journal name is contained in the list either in its full form (e.g Physical Review
     * Letters) or its abbreviated form (e.g. Phys. Rev. Lett.).
     */
    public boolean isKnownName(String journalName) {
        // check for at least one "?"
        if (QUESTION_MARK.matcher(journalName).find()) {
            return false;
        }

        String journal = journalName.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        boolean isKnown = customAbbreviations.stream().anyMatch(abbreviation -> isMatched(journal, abbreviation));
        if (isKnown) {
            return true;
        }

        return fullToAbbreviation.containsKey(journal) || abbreviationToFull.containsKey(journal)
                || findDottedAbbrFromDotless(journal).length() > 0;
    }

    /**
     * Returns true if the given journal name is in its abbreviated form (e.g. Phys. Rev. Lett.). The test is strict,
     * i.e. journals whose abbreviation is the same as the full name are not considered
     */
    public boolean isAbbreviatedName(String journalName) {
        String journal = journalName.trim();

        // journal abbreviation must be at least 2 words
        boolean isMoreThanTwoWords = journalName.split(" ").length >= 2;

        return customAbbreviations.stream().anyMatch(abbreviation -> isMatchedAbbreviated(journal, abbreviation))
                || abbreviationToFull.containsKey(journal)
                || (isMoreThanTwoWords && findDottedAbbrFromDotless(journal).length() > 0);
    }

    public String findDottedAbbrFromDotless(String journalName) {
        // check for at least one "?"
        if (QUESTION_MARK.matcher(journalName).find()) {
            return "UNKNOWN";
        }

        String foundKey = "";

        // check for a dot-less abbreviation
        if (!DOT.matcher(journalName).find()) {
            // use dot-less abbr to find full name using regex
            String[] journalSplit = journalName.split(" ");

            for (int i = 0; i < journalSplit.length; i++) {
                String word = journalSplit[i] + "[\\.\\s]*";
                journalSplit[i] = word;
            }

            String joined = String.join("", journalSplit);

            foundKey = abbreviationToFull.keySet().stream()
                                         .filter(s -> Pattern.compile(joined).matcher(s).find())
                                         .collect(Collectors.joining());
        }

        return foundKey;
    }

    /**
     * Attempts to get the abbreviation of the journal given.
     *
     * @param input The journal name (either abbreviated or full name).
     */
    public Optional<Abbreviation> get(String input) {
        String journal = input.trim().replaceAll(Matcher.quoteReplacement("\\&"), "&");

        Optional<Abbreviation> customAbbreviation = customAbbreviations.stream()
                                                                       .filter(abbreviation -> isMatched(journal, abbreviation))
                                                                       .findAny();
        if (customAbbreviation.isPresent()) {
            return customAbbreviation;
        }

        return Optional.ofNullable(fullToAbbreviation.get(journal))
                       .map(abbreviation -> new Abbreviation(journal, abbreviation))
                       .or(() -> {
                           String abbr = "";

                           // check for dot-less abbr
                           if (isKnownName(journal) && isAbbreviatedName(journal)) {
                               abbr = findDottedAbbrFromDotless(journal);
                           }

                           return Optional.ofNullable(abbreviationToFull.get(abbr.equals("") ? journal : abbr))
                                          .map(fullName -> new Abbreviation(fullName, journal));
                       });
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

    public Optional<String> getMedlineAbbreviation(String text) {
        return get(text).map(Abbreviation::getMedlineAbbreviation);
    }

    public Optional<String> getShortestUniqueAbbreviation(String text) {
        return get(text).map(Abbreviation::getShortestUniqueAbbreviation);
    }

    public Set<String> getFullNames() {
        return fullToAbbreviation.keySet();
    }

    public List<Abbreviation> getAllLoaded() {
        return fullToAbbreviation.entrySet().stream().map(entry ->
                new Abbreviation(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }
}
