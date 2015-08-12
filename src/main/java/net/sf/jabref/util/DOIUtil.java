/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.util;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DOIUtil {
    // Base URL
    public static final String DOI_LOOKUP_PREFIX = "http://doi.org/";
    // DOI-regexp provided by http://stackoverflow.com/a/10324802/873282
    // Some DOI's are not caught by the regexp in the above link, i.e. 10.1002/(SICI)1522-2594(199911)42:5<952::AID-MRM16>3.0.CO;2-S
    // Removed <> from non-permitted characters
    // TODO: We need more tests if the regexes behave correctly!
    // See http://www.doi.org/doi_handbook/2_Numbering.html#2.6
    private static final String REGEXP_PLAINDOI = "\\b(10[.][0-9]{4,}(?:[.][0-9]+)*/(?:(?![\"&\\'])\\S)+)\\b";
    private static final Pattern PATTERN_PLAINDOI = Pattern.compile(REGEXP_PLAINDOI);
    private static final String REGEXP_DOI_WITH_HTTP_PREFIX = "http[s]?://[^\\s]*?" + REGEXP_PLAINDOI;

    /**
     * Check if the String matches a plain DOI
     *
     * @param value the String to check
     * @return true if value contains a DOI
     */
    public static boolean isDOI(String value) {
        return value != null && value.matches(".*" + REGEXP_PLAINDOI + ".*");
    }

    /**
     * Check if the String matches a URI presentation of a DOI
     *
     * <example>
     *     The DOI name "10.1006/jmbi.1998.2354" would be made an actionable link as "http://doi.org/10.1006/jmbi.1998.2354".
     * </example>
     *
     * @param value the String to check
     * @return true if value contains a URI presentation of a DOI
     */
    public static boolean isURI(String value) {
        return value != null && value.matches(".*" + REGEXP_DOI_WITH_HTTP_PREFIX + ".*");
    }

    /**
     * Extract a plain DOI
     *
     * TODO: why not return null if no DOI is found?
     *
     * @param value the String containing the DOI
     * @return the first plain DOI in value. If no DOI exists, the complete string is returned.
     */
    public static String getDOI(String value) {
        Matcher matcher = PATTERN_PLAINDOI.matcher(value);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return value;
        }
    }

    /**
     * Return a URI presentation for a specific DOI
     *
     * TODO: this has problems when no doi is detected or is null.
     * This has always been unchecked and needs to be investigated!
     *
     * @param doi the DOI value
     * @return a URI representation of the DOI
     */
    public static String getURI(String doi) {
        return DOI_LOOKUP_PREFIX + getDOI(doi);
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
