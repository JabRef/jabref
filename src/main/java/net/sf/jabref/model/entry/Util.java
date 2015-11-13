package net.sf.jabref.model.entry;

import java.util.ArrayList;

public class Util {

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

    public static String capitalizeFirst(String toCapitalize) {
        // Make first character of String uppercase, and the
        // rest lowercase.
        if (toCapitalize.length() > 1) {
            return toCapitalize.substring(0, 1).toUpperCase() + toCapitalize.substring(1, toCapitalize.length()).toLowerCase();
        } else {
            return toCapitalize.toUpperCase();
        }

    }

    /**
     * Build a String array containing all those elements of all that are not
     * in subset.
     * @param all The array of all values.
     * @param subset The subset of values.
     * @return The remainder that is not part of the subset.
     */
    public static String[] getRemainder(String[] all, String[] subset) {
    	if (subset.length == 0) {
    		return all;
    	}
    	if (all.equals(subset)) {
    		return new String[0];
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
        return al.toArray(new String[al.size()]);
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
}
