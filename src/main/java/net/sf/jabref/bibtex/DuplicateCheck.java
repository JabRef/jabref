/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.bibtex;

import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This class contains utility method for duplicate checking of entries.
 */
public class DuplicateCheck {

    /*
     * Integer values for indicating result of duplicate check (for entries):
     *
     */
    static final int NOT_EQUAL = 0;
    static final int EQUAL = 1;
    static final int EMPTY_IN_ONE = 2;
    static final int EMPTY_IN_TWO = 3;
    static final int EMPTY_IN_BOTH = 4;

    public static double duplicateThreshold = 0.75; // The overall threshold to signal a duplicate pair
    // Non-required fields are investigated only if the required fields give a value within
    // the doubt range of the threshold:
    private static final double doubtRange = 0.05;

    private static final double reqWeight = 3; // Weighting of all required fields

    // Extra weighting of those fields that are most likely to provide correct duplicate detection:
    private static final HashMap<String, Double> fieldWeights = new HashMap<>();

    static {
        DuplicateCheck.fieldWeights.put("author", 2.5);
        DuplicateCheck.fieldWeights.put("editor", 2.5);
        DuplicateCheck.fieldWeights.put("title", 3.);
        DuplicateCheck.fieldWeights.put("journal", 2.);
    }


    /**
     * Checks if the two entries represent the same publication.
     *
     * @param one BibtexEntry
     * @param two BibtexEntry
     * @return boolean
     */
    public static boolean isDuplicate(BibtexEntry one, BibtexEntry two) {

        // First check if they are of the same type - a necessary condition:
        if (one.getType() != two.getType()) {
            return false;
        }

        // The check if they have the same required fields:
        String[] fields = one.getType().getRequiredFields().toArray(new String[0]);
        double[] req;
        if (fields == null) {
            req = new double[]{0., 0.};
        } else {
            req = DuplicateCheck.compareFieldSet(fields, one, two);
        }

        if (Math.abs(req[0] - DuplicateCheck.duplicateThreshold) > DuplicateCheck.doubtRange) {
            // Far from the threshold value, so we base our decision on the req. fields only
            return req[0] >= DuplicateCheck.duplicateThreshold;
        }
        // Close to the threshold value, so we take a look at the optional fields, if any:
        fields = one.getType().getOptionalFields().toArray(new String[0]);
        if (fields != null) {
            double[] opt = DuplicateCheck.compareFieldSet(fields, one, two);
            double totValue = ((DuplicateCheck.reqWeight * req[0] * req[1]) + (opt[0] * opt[1])) / ((req[1] * DuplicateCheck.reqWeight) + opt[1]);
            return totValue >= DuplicateCheck.duplicateThreshold;
        }
        return req[0] >= DuplicateCheck.duplicateThreshold;
    }

    private static double[] compareFieldSet(String[] fields, BibtexEntry one, BibtexEntry two) {
        double res = 0;
        double totWeights = 0.;
        for (String field : fields) {
            // Util.pr(":"+compareSingleField(fields[i], one, two));
            double weight;
            if (DuplicateCheck.fieldWeights.containsKey(field)) {
                weight = DuplicateCheck.fieldWeights.get(field);
            } else {
                weight = 1.0;
            }
            totWeights += weight;
            int result = DuplicateCheck.compareSingleField(field, one, two);
            //System.out.println("Field: "+fields[i]+": "+result);
            if (result == EQUAL) {
                res += weight;
            } else if (result == EMPTY_IN_BOTH) {
                totWeights -= weight;
            }
        }
        if (totWeights > 0) {
            return new double[]{res / totWeights, totWeights};
        }
        return new double[] {0.5, 0.0};
    }

    private static int compareSingleField(String field, BibtexEntry one, BibtexEntry two) {
        String s1 = one.getField(field);
        String s2 = two.getField(field);
        if (s1 == null) {
            if (s2 == null) {
                return EMPTY_IN_BOTH;
            }
            return EMPTY_IN_ONE;
        } else if (s2 == null) {
            return EMPTY_IN_TWO;
        }

        // Util.pr(field+": '"+s1+"' vs '"+s2+"'");
        if (field.equals("author") || field.equals("editor")) {
            // Specific for name fields.
            // Harmonise case:
            String auth1 = AuthorList.fixAuthor_lastNameOnlyCommas(s1, false).replaceAll(" and ", " ").toLowerCase();
            String auth2 = AuthorList.fixAuthor_lastNameOnlyCommas(s2, false).replaceAll(" and ", " ").toLowerCase();
            //System.out.println(auth1);
            //System.out.println(auth2);
            //System.out.println(correlateByWords(auth1, auth2));
            double similarity = DuplicateCheck.correlateByWords(auth1, auth2, false);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else if (field.equals("pages")) {
            // Pages can be given with a variety of delimiters, "-", "--", " - ", " -- ".
            // We do a replace to harmonize these to a simple "-":
            // After this, a simple test for equality should be enough:
            s1 = s1.replaceAll("[- ]+", "-");
            s2 = s2.replaceAll("[- ]+", "-");
            if (s1.equals(s2)) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else if (field.equals("journal")) {
            // We do not attempt to harmonize abbreviation state of the journal names,
            // but we remove periods from the names in case they are abbreviated with
            // and without dots:
            s1 = s1.replaceAll("\\.", "").toLowerCase();
            s2 = s2.replaceAll("\\.", "").toLowerCase();
            //System.out.println(s1+" :: "+s2);
            double similarity = DuplicateCheck.correlateByWords(s1, s2, true);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else {
            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();
            double similarity = DuplicateCheck.correlateByWords(s1, s2, false);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
            /*if (s1.trim().equals(s2.trim()))
                return Util.EQUAL;
            else
                return Util.NOT_EQUAL;*/
        }
    }

    public static double compareEntriesStrictly(BibtexEntry one, BibtexEntry two) {
        HashSet<String> allFields = new HashSet<>();// one.getAllFields());
        allFields.addAll(one.getFieldNames());
        allFields.addAll(two.getFieldNames());

        int score = 0;
        for (String field : allFields) {
            Object en = one.getField(field);
            Object to = two.getField(field);
            if (((en != null) && (to != null) && en.equals(to)) || ((en == null) && (to == null))) {
                score++;
            }
        }
        if (score == allFields.size()) {
            return 1.01; // Just to make sure we can
            // use score>1 without
            // trouble.
        }
        return (double) score / allFields.size();
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
            if (DuplicateCheck.isDuplicate(entry, other)) {
                return other; // Duplicate found.
            }
        }
        return null; // No duplicate found.
    }

    /**
     * Compare two strings on the basis of word-by-word correlation analysis.
     *
     * @param s1       The first string
     * @param s2       The second string
     * @param truncate if true, always truncate the longer of two words to be compared to
     *                 harmonize their length. If false, use interpolation to harmonize the strings.
     * @return a value in the interval [0, 1] indicating the degree of match.
     */
    static double correlateByWords(String s1, String s2, boolean truncate) {
        String[] w1 = s1.split("\\s");
        String[] w2 = s2.split("\\s");
        int n = Math.min(w1.length, w2.length);
        int misses = 0;
        for (int i = 0; i < n; i++) {
            /*if (!w1[i].equalsIgnoreCase(w2[i]))
                misses++;*/
            double corr = DuplicateCheck.correlateStrings(w1[i], w2[i], truncate);
            if (corr < 0.75) {
                misses++;
            }
        }
        double missRate = (double) misses / (double) n;
        return 1 - missRate;
    }

    private static double correlateStrings(String s1, String s2, boolean truncate) {
        int minLength = Math.min(s1.length(), s2.length());
        if (truncate && (minLength == 1)) {
            return s1.charAt(0) == s2.charAt(0) ? 1.0 : 0.0;
        } else if ((s1.length() == 1) && (s2.length() == 1)) {
            return s1.equals(s2) ? 1.0 : 0.0;
        } else if (minLength == 0) {
            return s1.isEmpty() && s2.isEmpty() ? 1.0 : 0;
        }

        // Convert strings to numbers and harmonize length in a method dependent on truncate:
        if (truncate) {
            // Harmonize length by truncation:
            if (s1.length() > minLength) {
                s1 = s1.substring(0, minLength);
            }
            if (s2.length() > minLength) {
                s2 = s2.substring(0, minLength);
            }
        }
        double[] n1 = DuplicateCheck.numberizeString(s1);
        double[] n2 = DuplicateCheck.numberizeString(s2);
        // If truncation is disabled, harmonize length by interpolation:
        if (!truncate) {
            if (n1.length < n2.length) {
                n1 = DuplicateCheck.stretchArray(n1, n2.length);
            } else if (n2.length < n1.length) {
                n2 = DuplicateCheck.stretchArray(n2, n1.length);
            }
        }
        return DuplicateCheck.corrCoef(n1, n2);
    }

    private static double corrCoef(double[] n1, double[] n2) {
        // Calculate mean values:
        double mean1 = 0;
        double mean2 = 0;
        for (int i = 0; i < n1.length; i++) {
            mean1 += n1[i];
            mean2 += n2[i];
        }
        mean1 /= n1.length;
        mean2 /= n2.length;
        double sigma1 = 0;
        double sigma2 = 0;
        // Calculate correlation coefficient:
        double corr = 0;
        for (int i = 0; i < n1.length; i++) {
            sigma1 += (n1[i] - mean1) * (n1[i] - mean1);
            sigma2 += (n2[i] - mean2) * (n2[i] - mean2);
            corr += (n1[i] - mean1) * (n2[i] - mean2);
        }
        sigma1 = Math.sqrt(sigma1);
        sigma2 = Math.sqrt(sigma2);
        if ((sigma1 > 0) && (sigma2 > 0)) {
            return corr / (sigma1 * sigma2);
        }
        return 0;
    }

    private static double[] numberizeString(String s) {
        double[] res = new double[s.length()];
        for (int i = 0; i < s.length(); i++) {
            res[i] = s.charAt(i);
        }
        return res;
    }

    private static double[] stretchArray(double[] array, int length) {
        if ((length <= array.length) || (array.length == 0)) {
            return array;
        }
        double multip = (double) array.length / (double) length;
        double[] newArray = new double[length];
        for (int i = 0; i < newArray.length; i++) {
            double index = i * multip;
            int baseInd = (int) Math.floor(index);
            double dist = index - Math.floor(index);
            newArray[i] = (dist * array[Math.min(array.length - 1, baseInd + 1)])
                    + ((1.0 - dist) * array[baseInd]);
        }
        return newArray;
    }

}
