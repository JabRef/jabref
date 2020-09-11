package org.jabref.logic.citationkeypattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the utility class of the LabelPattern package.
 */
public class CitationKeyGenerator extends BracketedPattern {
    /*
     * All single characters that we can use for extending a key to make it unique.
     */
    public static final String APPENDIX_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    public static final String DEFAULT_UNWANTED_CHARACTERS = "-`ʹ:!;?^+";
    private static final Logger LOGGER = LoggerFactory.getLogger(CitationKeyGenerator.class);
    // Source of disallowed characters : https://tex.stackexchange.com/a/408548/9075
    private static final List<Character> DISALLOWED_CHARACTERS = Arrays.asList('{', '}', '(', ')', ',', '=', '\\', '"', '#', '%', '~', '\'');
    private final AbstractCitationKeyPattern citeKeyPattern;
    private final BibDatabase database;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;

    public CitationKeyGenerator(BibDatabaseContext bibDatabaseContext, CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this(bibDatabaseContext.getMetaData().getCiteKeyPattern(citationKeyPatternPreferences.getKeyPattern()),
                bibDatabaseContext.getDatabase(),
                citationKeyPatternPreferences);
    }

    public CitationKeyGenerator(AbstractCitationKeyPattern citeKeyPattern, BibDatabase database, CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.citeKeyPattern = Objects.requireNonNull(citeKeyPattern);
        this.database = Objects.requireNonNull(database);
        this.citationKeyPatternPreferences = Objects.requireNonNull(citationKeyPatternPreferences);
    }

    @Deprecated
    static String generateKey(BibEntry entry, String pattern) {
        return generateKey(entry, pattern, new BibDatabase());
    }

    @Deprecated
    static String generateKey(BibEntry entry, String pattern, BibDatabase database) {
        GlobalCitationKeyPattern keyPattern = new GlobalCitationKeyPattern(Collections.emptyList());
        keyPattern.setDefaultValue("[" + pattern + "]");
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                keyPattern,
                ',');

        return new CitationKeyGenerator(keyPattern, database, patternPreferences).generateKey(entry);
    }

    /**
     * Computes an appendix to a citation key that could make it unique. We use
     * a-z for numbers 0-25, and then aa-az, ba-bz, etc.
     *
     * @param number The appendix number.
     * @return The String to append.
     */
    private static String getAppendix(int number) {
        if (number >= APPENDIX_CHARACTERS.length()) {
            int lastChar = number % APPENDIX_CHARACTERS.length();
            return getAppendix((number / APPENDIX_CHARACTERS.length()) - 1) + APPENDIX_CHARACTERS.substring(lastChar, lastChar + 1);
        } else {
            return APPENDIX_CHARACTERS.substring(number, number + 1);
        }
    }

    public static String removeUnwantedCharacters(String key, String unwantedCharacters) {
        String newKey = key.chars()
                           .filter(c -> unwantedCharacters.indexOf(c) == -1)
                           .filter(c -> !DISALLOWED_CHARACTERS.contains((char) c))
                           .collect(StringBuilder::new,
                                   StringBuilder::appendCodePoint, StringBuilder::append)
                           .toString();

        // Replace non-English characters like umlauts etc. with a sensible
        // letter or letter combination that bibtex can accept.
        return StringUtil.replaceSpecialCharacters(newKey);
    }

    public static String cleanKey(String key, String unwantedCharacters) {
        return removeUnwantedCharacters(key, unwantedCharacters).replaceAll("\\s", "");
    }

    public String generateKey(BibEntry entry) {
        String key;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // get the type of entry
            EntryType entryType = entry.getType();
            // Get the arrayList corresponding to the type
            List<String> typeList = new ArrayList<>(citeKeyPattern.getValue(entryType));
            if (!typeList.isEmpty()) {
                typeList.remove(0);
            }
            boolean field = false;
            for (String typeListEntry : typeList) {
                if ("[".equals(typeListEntry)) {
                    field = true;
                } else if ("]".equals(typeListEntry)) {
                    field = false;
                } else if (field) {
                    // check whether there is a modifier on the end such as
                    // ":lower"
                    List<String> parts = parseFieldMarker(typeListEntry);
                    Character delimiter = citationKeyPatternPreferences.getKeywordDelimiter();
                    String pattern = "[" + parts.get(0) + "]";
                    String label = expandBrackets(pattern, delimiter, entry, database);
                    // apply modifier if present
                    if (parts.size() > 1) {
                        label = applyModifiers(label, parts, 1);
                    }
                    // Remove all illegal characters from the label.
                    label = cleanKey(label, citationKeyPatternPreferences.getUnwantedCharacters());
                    stringBuilder.append(label);
                } else {
                    stringBuilder.append(typeListEntry);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot make label", e);
        }

        key = stringBuilder.toString();

        // Remove Regular Expressions while generating Keys
        String regex = citationKeyPatternPreferences.getKeyPatternRegex();
        if ((regex != null) && !regex.trim().isEmpty()) {
            String replacement = citationKeyPatternPreferences.getKeyPatternReplacement();
            key = key.replaceAll(regex, replacement);
        }

        String oldKey = entry.getCiteKeyOptional().orElse(null);
        long occurrences = database.getNumberOfKeyOccurrences(key);

        if (Objects.equals(oldKey, key)) {
            occurrences--; // No change, so we can accept one dupe.
        }

        boolean alwaysAddLetter = citationKeyPatternPreferences.getKeySuffix()
                == CitationKeyPatternPreferences.KeySuffix.ALWAYS;

        boolean firstLetterA = citationKeyPatternPreferences.getKeySuffix()
                == CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A;

        String newKey;
        if (!alwaysAddLetter && (occurrences == 0)) {
            newKey = key;
        } else {
            // The key is already in use, so we must modify it.
            int number = !alwaysAddLetter && !firstLetterA ? 1 : 0;
            String moddedKey;

            do {
                moddedKey = key + getAppendix(number);
                number++;

                occurrences = database.getNumberOfKeyOccurrences(moddedKey);
                // only happens if #getAddition() is buggy
                if (Objects.equals(oldKey, moddedKey)) {
                    occurrences--;
                }
            } while (occurrences > 0);

            newKey = moddedKey;
        }
        return newKey;
    }

    /**
     * Generates a citation key for the given entry, and sets the key.
     *
     * @param entry the entry to generate the key for
     * @return the change to the key (or an empty optional if the key was not changed)
     */
    public Optional<FieldChange> generateAndSetKey(BibEntry entry) {
        String newKey = generateKey(entry);
        return entry.setCiteKey(newKey);
    }
}
