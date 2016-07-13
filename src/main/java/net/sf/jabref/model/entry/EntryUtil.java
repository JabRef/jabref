package net.sf.jabref.model.entry;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class EntryUtil {

    public static final String SEPARATING_CHARS_NOSPACE = ";,\n";


    /**
     * Make first character of String uppercase, and the
     * rest lowercase.
     */
    public static String capitalizeFirst(String toCapitalize) {
        if (toCapitalize.length() > 1) {
            return toCapitalize.substring(0, 1).toUpperCase()
                    + toCapitalize.substring(1, toCapitalize.length()).toLowerCase();
        } else {
            return toCapitalize.toUpperCase();
        }

    }

    /**
     * @param keywordString a String of keywords
     * @return an List containing the keywords. An empty list if keywords are null or empty
     */
    public static Set<String> getSeparatedKeywords(String keywordString) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        if (keywordString == null) {
            return keywords;
        }
        // _NOSPACE is a hack to support keywords such as "choreography transactions"
        // a more intelligent algorithm would check for the separator chosen (SEPARATING_CHARS_NOSPACE)
        // if nothing is found, " " is likely to be the separating char.
        // solution by RisKeywords.java: s.split(",[ ]*")
        StringTokenizer tok = new StringTokenizer(keywordString, SEPARATING_CHARS_NOSPACE);
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken().trim();
            keywords.add(word);
        }
        return keywords;
    }

    /**
     * @param entry a BibEntry
     * @return an List containing the keywords of the entry. An empty list if keywords are null or empty
     */
    public static Set<String> getSeparatedKeywords(BibEntry entry) {
        return getSeparatedKeywords(entry.getFieldOptional(BibEntry.KEYWORDS_FIELD).orElse(null));
    }

    /**
     * Returns a list of words contained in the given text.
     * Whitespace, comma and semicolon are considered as separator between words.
     *
     * @param text the input
     * @return a list of words
     */
    public static List<String> getStringAsWords(String text) {
        return Arrays.asList(text.split("[\\s,;]+"));
    }
}
