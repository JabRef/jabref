package net.sf.jabref;

import java.util.HashSet;
import java.util.Iterator;

/**
 * This class contains utility method for duplicate checking of entries.
 */
public class DuplicateCheck {


    /**
     * Checks if the two entries represent the same publication.
     *
     * @param one BibtexEntry
     * @param two BibtexEntry
     * @return boolean
     */
    public static boolean isDuplicate(BibtexEntry one, BibtexEntry two) {

        // First check if they are of the same type - a necessary condition:
        if (one.getType() != two.getType())
            return false;

        // The check if they have the same required fields:
        String[] fields = one.getType().getRequiredFields();

        float req, reqWeight = 2;
        if (fields == null) {
            req = 0;
            reqWeight = 0;
        }
        else
            req = compareFieldSet(fields, one, two);
        fields = one.getType().getOptionalFields();

        if (fields != null) {
            float opt = compareFieldSet(fields, one, two);
            return (reqWeight * req + opt) / (1 + reqWeight) >= Globals.duplicateThreshold;
        } else {
            return (req >= Globals.duplicateThreshold);
        }
    }

    private static float compareFieldSet(String[] fields, BibtexEntry one, BibtexEntry two) {
        int res = 0, empty = 0;
        for (int i = 0; i < fields.length; i++) {
            // Util.pr(":"+compareSingleField(fields[i], one, two));
            int result = compareSingleField(fields[i], one, two);
            if (result == Util.EQUAL) {
                res++;
                // Util.pr(fields[i]);
            }
            else if (result == Util.EMPTY_IN_BOTH)
                empty++;
        }
        if (fields.length > empty)
            return ((float) res) / ((float) (fields.length - empty));
        else // no fields present. This points to a possible duplicate?
            return 0.5f;
    }

    private static int compareSingleField(String field, BibtexEntry one, BibtexEntry two) {
        String s1 = one.getField(field), s2 = two.getField(field);
        if (s1 == null) {
            if (s2 == null)
                return Util.EMPTY_IN_BOTH;
            else
                return Util.EMPTY_IN_ONE;
        } else if (s2 == null)
            return Util.EMPTY_IN_TWO;
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();
        // Util.pr(field+": '"+s1+"' vs '"+s2+"'");
        if (field.equals("author") || field.equals("editor")) {
            // Specific for name fields.
            // Harmonise case:
            String[] aus1 = AuthorList.fixAuthor_lastNameFirst(s1).split(" and "), aus2 = AuthorList
                    .fixAuthor_lastNameFirst(s2).split(" and "), au1 = aus1[0].split(","), au2 = aus2[0]
                    .split(",");

            // Can check number of authors, all authors or only the first.
            if ((aus1.length > 0) && (aus1.length == aus2.length)
                    && au1[0].trim().equals(au2[0].trim()))
                return Util.EQUAL;
            else
                return Util.NOT_EQUAL;
        } else {
            double similarity = correlateByWords(s1, s2);
            if (similarity > 0.8)
                return Util.EQUAL;
            else
                return Util.NOT_EQUAL;
            /*if (s1.trim().equals(s2.trim()))
                return Util.EQUAL;
            else
                return Util.NOT_EQUAL;*/
        }

    }

    public static double compareEntriesStrictly(BibtexEntry one, BibtexEntry two) {
        HashSet<String> allFields = new HashSet<String>();// one.getAllFields());
        allFields.addAll(one.getAllFields());
        allFields.addAll(two.getAllFields());

        int score = 0;
        for (Iterator<String> fld = allFields.iterator(); fld.hasNext();) {
            String field = fld.next();
            Object en = one.getField(field), to = two.getField(field);
            if ((en != null) && (to != null) && (en.equals(to)))
                score++;
            else if ((en == null) && (to == null))
                score++;
        }
        if (score == allFields.size())
            return 1.01; // Just to make sure we can
            // use score>1 without
            // trouble.
        else
            return ((double) score) / allFields.size();
    }

    /**
     * Goes through all entries in the given database, and if at least one of
     * them is a duplicate of the given entry, as per
     * Util.isDuplicate(BibtexEntry, BibtexEntry), the duplicate is returned.
     * The search is terminated when the first duplicate is found.
     *
     * @param database The database to search.
     * @param entry    The entry of which we are looking for duplicates.
     * @return The first duplicate entry found. null if no duplicates are found.
     */
    public static BibtexEntry containsDuplicate(BibtexDatabase database, BibtexEntry entry) {
        for (BibtexEntry other : database.getEntries()) {
            if (isDuplicate(entry, other))
                return other; // Duplicate found.
        }
        return null; // No duplicate found.
	}

    public static double correlateByWords(String s1, String s2) {
        String[] w1 = s1.split("\\s"),
                w2 = s2.split("\\s");
        int n = Math.min(w1.length, w2.length);
        int misses = 0;
        for (int i=0; i<n; i++) {
            double corr = correlateStrings(w1[i], w2[i]);
            if (corr < 0.75)
                misses++;
        }
        double missRate = ((double)misses)/((double)n);
        return 1-missRate;
    }

    public static double correlateStrings(String s1, String s2) {
        if (s1.length() == 1 && s2.length() == 1) {
            return s1.equals(s2) ? 1.0 : 0.0;
        }
        // Convert strings to numbers and pad the shortest one with zeros:
        double[] n1 = numberizeString(s1),
                n2 = numberizeString(s2);
        if (n1.length < n2.length)
            n1 = stretchArray(n1, n2.length);
        else if (n2.length < n1.length)
            n2 = stretchArray(n2, n1.length);
        return corrCoef(n1, n2);
    }

    private static double corrCoef(double[] n1, double[] n2) {
        // Calculate mean values:
        double mean1 = 0, mean2 = 0;
        for (int i=0; i<n1.length; i++) {
            mean1 += n1[i];
            mean2 += n2[i];
        }
        mean1 /= (double)n1.length;
        mean2 /= (double)n2.length;
        double sigma1 = 0, sigma2 = 0;
        // Calculate correlation coefficient:
        double corr = 0;
        for (int i=0; i<n1.length; i++) {
            sigma1 += (n1[i] - mean1)*(n1[i] - mean1);
            sigma2 += (n2[i] - mean2)*(n2[i] - mean2);
            corr += (n1[i] - mean1)*(n2[i] - mean2);
        }
        sigma1 = Math.sqrt(sigma1);
        sigma2 = Math.sqrt(sigma2);
        if (sigma1 > 0 && sigma2 > 0)
            return corr/(sigma1*sigma2);
        else
            return 0;
    }


    private static double[] numberizeString(String s) {
        double[] res = new double[s.length()];
        for (int i=0; i<s.length(); i++)
            res[i] = (double)s.charAt(i);
        return res;
    }

    private static double[] stretchArray(double[] array, int length) {
        if (length <= array.length)
            return array;
        double multip = ((double)array.length)/((double)length);
        double[] newArray = new double[length];
        for (int i=0; i<newArray.length; i++) {
            double index = ((double)i)*multip;
            int baseInd = (int)Math.floor(index);
            double dist = index - Math.floor(index);
            newArray[i] = dist*array[Math.min(array.length-1, baseInd+1)]
                + (1.0 - dist)*array[baseInd];
        }
        return newArray;
    }


    public static void main(String[] args) {
        String d1 =  "Characterization of Calanus finmarchicus habitat in the North Sea",
                d2 = "Characterization of Calunus finmarchicus habitat in the North Sea",
                d3 = "Characterization of Calanus glacialissss habitat in the South Sea";
        System.out.println(correlateByWords(d1, d2));
        System.out.println(correlateByWords(d1, d3));
        System.out.println(correlateByWords(d2, d3));

    }
}
