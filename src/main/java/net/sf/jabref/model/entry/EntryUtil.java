package net.sf.jabref.model.entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class EntryUtil {

    public static final String SEPARATING_CHARS_NOSPACE = ";,\n";


    /**
     * Static equals that can also return the right result when one of the objects is null.
     *
     * @param one The object whose equals method is called if the first is not null.
     * @param two The object passed to the first one if the first is not null.
     * @return <code>one == null ? two == null : one.equals(two);</code>
     */
    public static boolean equals(Object one, Object two) {
        return one == null ? two == null : one.equals(two);
    }

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
     * Build a String array containing all those elements of all that are not
     * in subset.
     *
     * @param all The array of all values.
     * @param subset The subset of values.
     * @return The remainder that is not part of the subset. - The result MUST NOT be modified
     */
    public static List<String> getRemainder(List<String> all, List<String> subset) {
        if (subset.isEmpty()) {
            // ensure that "all" does not get modified
            return Collections.unmodifiableList(all);
        }
        if (all.equals(subset)) {
            return Collections.EMPTY_LIST;
        }

        ArrayList<String> al = new ArrayList<>();
        for (String anAll : all) {
            boolean found = false;
            for (String aSubset : subset) {
                if (aSubset.equals(anAll)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                al.add(anAll);
            }
        }
        return al;
    }

    /**
	 * Concatenate two String arrays
	 *
	 * @param array1
	 *            the first string array
	 * @param array2
	 *            the second string array
	 * @return The concatenation of array1 and array2
	 */
	public static String[] arrayConcat(String[] array1, String[] array2) {
		int len1 = array1.length;
		int len2 = array2.length;
		String[] union = new String[len1 + len2];
		System.arraycopy(array1, 0, union, 0, len1);
		System.arraycopy(array2, 0, union, len1, len2);
		return union;
	}

    /**
     * @param keywords a String of keywords
     * @return an ArrayList containing the keywords. An emtpy list if keywords are null or empty
     */
    public static ArrayList<String> getSeparatedKeywords(String keywords) {
        ArrayList<String> res = new ArrayList<>();
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
