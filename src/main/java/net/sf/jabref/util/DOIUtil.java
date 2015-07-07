package net.sf.jabref.util;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOIUtil {

    // DOI-regexp provided by http://stackoverflow.com/a/10324802/873282
    // Some DOI's are not caught by the regexp in the above link, i.e. 10.1002/(SICI)1522-2594(199911)42:5<952::AID-MRM16>3.0.CO;2-S
    // Removed <> from non-permitted characters
    private static final String REGEXP_PLAINDOI = "\\b(10[.][0-9]{4,}(?:[.][0-9]+)*/(?:(?![\"&\\'])\\S)+)\\b";
    private static final Pattern PATTERN_PLAINDOI = Pattern.compile(REGEXP_PLAINDOI);
    private static final String REGEXP_DOI_WITH_HTTP_PREFIX = "http[s]?://[^\\s]*?" + REGEXP_PLAINDOI;

    /**
     * Check if the String matches a DOI (with http://...)
     */
    public static boolean checkForDOIwithHTTPprefix(String check) {
        return (check != null) && check.matches(".*" + REGEXP_DOI_WITH_HTTP_PREFIX + ".*");
    }

    /**
     *
     * @param check - string to check
     * @return true if "check" contains a DOI
     */
    public static boolean checkForPlainDOI(String check) {
        return (check != null) && check.matches(".*" + REGEXP_PLAINDOI + ".*");
    }

    /**
     * Remove the http://... from DOI
     *
     * @param doi - may not be null
     * @return first DOI in the given String (without http://... prefix). If no DOI exists, the complete string is returned
     */
    public static String getDOI(String doi) {
        Matcher matcher = PATTERN_PLAINDOI.matcher(doi);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return doi;
        }
    }

    public static void removeDOIfromBibtexEntryField(BibtexEntry bes, String fieldName, NamedCompound ce) {
        String origValue = bes.getField(fieldName);
        String value = origValue;
        value = value.replaceAll(REGEXP_DOI_WITH_HTTP_PREFIX, "");
        value = value.replaceAll(REGEXP_PLAINDOI, "");
        value = value.trim();
        if (value.isEmpty()) {
            value = null;
        }
        if (!origValue.equals(value)) {
            ce.addEdit(new UndoableFieldChange(bes, fieldName, origValue, value));
            bes.setField(fieldName, value);
        }
    }
}
