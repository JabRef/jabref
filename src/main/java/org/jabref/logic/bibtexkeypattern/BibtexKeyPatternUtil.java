package org.jabref.logic.bibtexkeypattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.util.BracketedExpressionExpander;
import org.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This is the utility class of the LabelPattern package.
 */
public class BibtexKeyPatternUtil extends BracketedExpressionExpander {
    private static final Log LOGGER = LogFactory.getLog(BibtexKeyPatternUtil.class);

    // All single characters that we can use for extending a key to make it unique:
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";

    private BibtexKeyPatternUtil() {
    }

    /**
     * Generates a BibTeX label according to the pattern for a given entry type, and saves the unique label in the
     * <code>Bibtexentry</code>.
     *
     * The given database is used to avoid duplicate keys.
     *
     * @param citeKeyPattern
     * @param database a <code>BibDatabase</code>
     * @param entry a <code>BibEntry</code>
     * @return modified BibEntry
     */
    public static void makeAndSetLabel(AbstractBibtexKeyPattern citeKeyPattern, BibDatabase database, BibEntry entry,
            BibtexKeyPatternPreferences bibtexKeyPatternPreferences) {
        String newKey = makeLabel(citeKeyPattern, database, entry, bibtexKeyPatternPreferences);
        entry.setCiteKey(newKey);
    }

    private static String makeLabel(AbstractBibtexKeyPattern citeKeyPattern, BibDatabase database, BibEntry entry, BibtexKeyPatternPreferences bibtexKeyPatternPreferences) {
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
        key = checkLegalKey(stringBuilder.toString(), bibtexKeyPatternPreferences.isEnforceLegalKey());

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
                moddedKey = key + getAddition(number);
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

    public static String makeLabel(BibEntry entry, String value, Character keywordDelimiter, BibDatabase database) {
        return expandBrackets("[" + value + "]", keywordDelimiter, entry, database);
    }

    /**
     * Computes an appendix to a BibTeX key that could make it unique. We use
     * a-z for numbers 0-25, and then aa-az, ba-bz, etc.
     *
     * @param number
     *            The appendix number.
     * @return The String to append.
     */
    private static String getAddition(int number) {
        if (number >= CHARS.length()) {
            int lastChar = number % CHARS.length();
            return getAddition((number / CHARS.length()) - 1) + CHARS.substring(lastChar, lastChar + 1);
        } else {
            return CHARS.substring(number, number + 1);
        }
    }

    /**
     * This method returns a String similar to the one passed in, except that it is molded into a form that is
     * acceptable for bibtex.
     * <p>
     * Watch-out that the returned string might be of length 0 afterwards.
     *
     * @param key             mayBeNull
     * @param enforceLegalKey make sure that the key is legal in all respects
     */
    public static String checkLegalKey(String key, boolean enforceLegalKey) {
        if (key == null) {
            return null;
        }
        if (!enforceLegalKey) {
            // User doesn't want us to enforce legal characters. We must still look
            // for whitespace and some characters such as commas, since these would
            // interfere with parsing:
            StringBuilder newKey = new StringBuilder();
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (!Character.isWhitespace(c) && ("{}(),\\\"".indexOf(c) == -1)) {
                    newKey.append(c);
                }
            }
            return newKey.toString();
        }

        StringBuilder newKey = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isWhitespace(c) && ("{}(),\\\"#~^'".indexOf(c) == -1)) {
                newKey.append(c);
            }
        }

        // Replace non-English characters like umlauts etc. with a sensible
        // letter or letter combination that bibtex can accept.

        return StringUtil.replaceSpecialCharacters(newKey.toString());
    }

    public static String makeLabel(BibDatabaseContext bibDatabaseContext,
            BibEntry entry,
            BibtexKeyPatternPreferences bibtexKeyPatternPreferences) {
        AbstractBibtexKeyPattern citeKeyPattern = bibDatabaseContext.getMetaData().getCiteKeyPattern(bibtexKeyPatternPreferences.getKeyPattern());
        return makeLabel(citeKeyPattern, bibDatabaseContext.getDatabase(), entry, bibtexKeyPatternPreferences);
    }
}
