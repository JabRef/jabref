package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.List;
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
     * @param keywords a String of keywords
     * @return an List containing the keywords. An empty list if keywords are null or empty
     */
    public static List<String> getSeparatedKeywords(String keywords) {
        List<String> res = new ArrayList<>();
        if (keywords == null) {
            return res;
        }
        // _NOSPACE is a hack to support keywords such as "choreography transactions"
        // a more intelligent algorithm would check for the separator chosen (SEPARATING_CHARS_NOSPACE)
        // if nothing is found, " " is likely to be the separating char.
        // solution by RisKeywords.java: s.split(",[ ]*")
        StringTokenizer tok = new StringTokenizer(keywords, SEPARATING_CHARS_NOSPACE);
        while (tok.hasMoreTokens()) {
            String word = tok.nextToken().trim();
            res.add(word);
        }
        return res;
    }
}
