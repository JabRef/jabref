package net.sf.jabref.model.entry;

import java.util.Arrays;
import java.util.List;

public class EntryUtil {

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
