package org.jabref.logic.citationkeypattern;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

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
    private final String unwantedCharacters;

    public CitationKeyGenerator(BibDatabaseContext bibDatabaseContext, CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this(bibDatabaseContext.getMetaData().getCiteKeyPattern(citationKeyPatternPreferences.getKeyPattern()),
                bibDatabaseContext.getDatabase(),
                citationKeyPatternPreferences);
    }

    public CitationKeyGenerator(AbstractCitationKeyPattern citeKeyPattern, BibDatabase database, CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.citeKeyPattern = Objects.requireNonNull(citeKeyPattern);
        this.database = Objects.requireNonNull(database);
        this.citationKeyPatternPreferences = Objects.requireNonNull(citationKeyPatternPreferences);
        this.unwantedCharacters = citationKeyPatternPreferences.getUnwantedCharacters();
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
     * Computes an appendix to a citation key that could make it unique. We use a-z for numbers 0-25, and then aa-az, ba-bz, etc.
     *
     * @param number The appendix number.
     * @return The String to append.
     */
    private static String getAppendix(int number) {
        if (number >= APPENDIX_CHARACTERS.length()) {
            int lastChar = number % APPENDIX_CHARACTERS.length();
            return getAppendix((number / APPENDIX_CHARACTERS.length()) - 1) + APPENDIX_CHARACTERS.charAt(lastChar);
        } else {
            return APPENDIX_CHARACTERS.substring(number, number + 1);
        }
    }

    public static String removeDefaultUnwantedCharacters(String key) {
        return removeUnwantedCharacters(key, DEFAULT_UNWANTED_CHARACTERS);
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

    /**
     * Generate a citation key for the given {@link BibEntry}.
     *
     * @param entry a {@link BibEntry}
     * @return a citation key based on the user's preferences
     */
    public String generateKey(BibEntry entry) {
        Objects.requireNonNull(entry);
        String currentKey = entry.getCitationKey().orElse(null);

        String newKey = createCitationKeyFromPattern(entry);
        newKey = replaceWithRegex(newKey);
        newKey = appendLettersToKey(newKey, currentKey);
        return cleanKey(newKey, unwantedCharacters);
    }

    /**
     * A letter will be appended to the key based on the user's preferences, either always or to prevent duplicated keys.
     *
     * @param key    the new key
     * @param oldKey the old key
     * @return a key, if needed, with an appended letter
     */
    private String appendLettersToKey(String key, String oldKey) {
        long occurrences = database.getNumberOfCitationKeyOccurrences(key);

        if (Objects.equals(oldKey, key)) {
            occurrences--; // No change, so we can accept one dupe.
        }

        boolean alwaysAddLetter = citationKeyPatternPreferences.getKeySuffix()
                == CitationKeyPatternPreferences.KeySuffix.ALWAYS;

        if (alwaysAddLetter || occurrences != 0) {
            // The key is already in use, so we must modify it.
            boolean firstLetterA = citationKeyPatternPreferences.getKeySuffix()
                    == CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A;

            int number = !alwaysAddLetter && !firstLetterA ? 1 : 0;
            String moddedKey;

            do {
                moddedKey = key + getAppendix(number);
                number++;

                occurrences = database.getNumberOfCitationKeyOccurrences(moddedKey);
                // only happens if #getAddition() is buggy
                if (Objects.equals(oldKey, moddedKey)) {
                    occurrences--;
                }
            } while (occurrences > 0);

            key = moddedKey;
        }
        return key;
    }

    /**
     * Using preferences, replace matches to the provided regex with a string.
     *
     * @param key the citation key
     * @return the citation key where matches to the regex are replaced
     */
    private String replaceWithRegex(String key) {
        // Remove Regular Expressions while generating Keys
        String regex = citationKeyPatternPreferences.getKeyPatternRegex();
        if ((regex != null) && !regex.trim().isEmpty()) {
            String replacement = citationKeyPatternPreferences.getKeyPatternReplacement();
            try {
                key = key.replaceAll(regex, replacement);
            } catch (PatternSyntaxException e) {
                LOGGER.warn("There is a syntax error in the regular expression \"{}\" used to generate a citation key", regex, e);
            }
        }
        return key;
    }

    private String createCitationKeyFromPattern(BibEntry entry) {
        // get the type of entry
        EntryType entryType = entry.getType();
        // Get the arrayList corresponding to the type
        List<String> citationKeyPattern = citeKeyPattern.getValue(entryType);
        if (citationKeyPattern.isEmpty()) {
            return "";
        }
        return expandBrackets(citationKeyPattern.get(0), expandBracketContent(entry));
    }

    /**
     * A helper method to create a {@link Function} that takes a single bracketed expression, expands it, and cleans the key.
     *
     * @param entry the {@link BibEntry} that a citation key is generated for
     * @return a cleaned citation key for the given {@link BibEntry}
     */
    private Function<String, String> expandBracketContent(BibEntry entry) {
        Character keywordDelimiter = citationKeyPatternPreferences.getKeywordDelimiter();

        return (String bracket) -> {
            String expandedPattern;
            List<String> fieldParts = parseFieldAndModifiers(bracket);

            expandedPattern = removeUnwantedCharacters(getFieldValue(entry, fieldParts.get(0), keywordDelimiter, database), unwantedCharacters);
            // check whether there is a modifier on the end such as
            // ":lower":
            if (fieldParts.size() > 1) {
                // apply modifiers:
                expandedPattern = applyModifiers(expandedPattern, fieldParts, 1, expandBracketContent(entry));
            }
            return cleanKey(expandedPattern, unwantedCharacters);
        };
    }

    /**
     * Generates a citation key for the given entry, and sets the key.
     *
     * @param entry the entry to generate the key for
     * @return the change to the key (or an empty optional if the key was not changed)
     */
    public Optional<FieldChange> generateAndSetKey(BibEntry entry) {
        String newKey = generateKey(entry);
        return entry.setCitationKey(newKey);
    }
}
