package org.jabref.logic.bibtexkeypattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the utility class of the LabelPattern package.
 */
public class BibtexKeyGenerator extends BracketedPattern {
    /*
     * All single characters that we can use for extending a key to make it unique.
     */
    public static final String APPENDIX_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexKeyGenerator.class);
    private static final String KEY_ILLEGAL_CHARACTERS = "{}(),\\\"#~^':`";
    private static final String KEY_UNWANTED_CHARACTERS = "{}(),\\\"";
    private final AbstractBibtexKeyPattern citeKeyPattern;
    private final BibDatabase database;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;

    public BibtexKeyGenerator(BibDatabaseContext bibDatabaseContext, BibtexKeyPatternPreferences bibtexKeyPatternPreferences) {
        this(bibDatabaseContext.getMetaData().getCiteKeyPattern(bibtexKeyPatternPreferences.getKeyPattern()),
                bibDatabaseContext.getDatabase(),
                bibtexKeyPatternPreferences);
    }

    public BibtexKeyGenerator(AbstractBibtexKeyPattern citeKeyPattern, BibDatabase database, BibtexKeyPatternPreferences bibtexKeyPatternPreferences) {
        this.citeKeyPattern = Objects.requireNonNull(citeKeyPattern);
        this.database = Objects.requireNonNull(database);
        this.bibtexKeyPatternPreferences = Objects.requireNonNull(bibtexKeyPatternPreferences);
    }

    static String generateKey(BibEntry entry, String pattern) {
        return generateKey(entry, pattern, new BibDatabase());
    }

    static String generateKey(BibEntry entry, String pattern, BibDatabase database) {
        GlobalBibtexKeyPattern keyPattern = new GlobalBibtexKeyPattern(Collections.emptyList());
        keyPattern.setDefaultValue("[" + pattern + "]");
        return new BibtexKeyGenerator(keyPattern, database, new BibtexKeyPatternPreferences("", "", false, true, true, keyPattern, ','))
                .generateKey(entry);
    }

    /**
     * Computes an appendix to a BibTeX key that could make it unique. We use
     * a-z for numbers 0-25, and then aa-az, ba-bz, etc.
     *
     * @param number
     *            The appendix number.
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

    public static String cleanKey(String key, boolean enforceLegalKey) {
        if (!enforceLegalKey) {
            // User doesn't want us to enforce legal characters. We must still look
            // for whitespace and some characters such as commas, since these would
            // interfere with parsing:
            StringBuilder newKey = new StringBuilder();
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (!Character.isWhitespace(c) && (KEY_UNWANTED_CHARACTERS.indexOf(c) == -1)) {
                    newKey.append(c);
                }
            }
            return newKey.toString();
        }

        StringBuilder newKey = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isWhitespace(c) && (KEY_ILLEGAL_CHARACTERS.indexOf(c) == -1)) {
                newKey.append(c);
            }
        }

        // Replace non-English characters like umlauts etc. with a sensible
        // letter or letter combination that bibtex can accept.
        return StringUtil.replaceSpecialCharacters(newKey.toString());
    }

    public String generateKey(BibEntry entry) {
        String key;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            // get the type of entry
            String entryType = entry.getType();
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
                    Character delimiter = bibtexKeyPatternPreferences.getKeywordDelimiter();
                    String pattern = "[" + parts.get(0) + "]";
                    String label = expandBrackets(pattern, delimiter, entry, database);
                    // apply modifier if present
                    if (parts.size() > 1) {
                        label = applyModifiers(label, parts, 1);
                    }

                    stringBuilder.append(label);

                } else {
                    stringBuilder.append(typeListEntry);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Cannot make label", e);
        }

        // Remove all illegal characters from the key.
        key = cleanKey(stringBuilder.toString(), bibtexKeyPatternPreferences.isEnforceLegalKey());

        // Remove Regular Expressions while generating Keys
        String regex = bibtexKeyPatternPreferences.getKeyPatternRegex();
        if ((regex != null) && !regex.trim().isEmpty()) {
            String replacement = bibtexKeyPatternPreferences.getKeyPatternReplacement();
            key = key.replaceAll(regex, replacement);
        }

        String oldKey = entry.getCiteKeyOptional().orElse(null);
        int occurrences = database.getDuplicationChecker().getNumberOfKeyOccurrences(key);

        if (Objects.equals(oldKey, key)) {
            occurrences--; // No change, so we can accept one dupe.
        }

        boolean alwaysAddLetter = bibtexKeyPatternPreferences.isAlwaysAddLetter();
        boolean firstLetterA = bibtexKeyPatternPreferences.isFirstLetterA();

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

                occurrences = database.getDuplicationChecker().getNumberOfKeyOccurrences(moddedKey);
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
     * Generates a BibTeX key for the given entry, and sets the key.
     *
     * @param entry the entry to generate the key for
     * @return the change to the key (or an empty optional if the key was not changed)
     */
    public Optional<FieldChange> generateAndSetKey(BibEntry entry) {
        String newKey = generateKey(entry);
        return entry.setCiteKey(newKey);
    }
}
