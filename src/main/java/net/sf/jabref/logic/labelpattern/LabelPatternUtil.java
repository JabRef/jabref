/*  Copyright (C) 2003-2015 JabRef contributors.
                  2003-2015 Ulrik Stervbo (ulriks AT ruc.dk)

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
package net.sf.jabref.logic.labelpattern;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.formatter.casechanger.Word;
import net.sf.jabref.logic.layout.format.RemoveLatexCommands;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This is the utility class of the LabelPattern package.
 */
public class LabelPatternUtil {

    private static final String STARTING_CAPITAL_PATTERN = "[^A-Z]";

    // All single characters that we can use for extending a key to make it unique:
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";

    private static final Log LOGGER = LogFactory.getLog(LabelPatternUtil.class);

    private static final Pattern REGEX_PATTERN = Pattern.compile(".*\\(\\{([A-Z]+)\\}\\).*");

    private static List<String> defaultLabelPattern;

    private static final int CHARS_OF_FIRST = 5;


    static {
        updateDefaultPattern();
    }

    private static BibDatabase database;

    public static void updateDefaultPattern() {
        defaultLabelPattern = LabelPatternUtil
                .split(JabRefPreferences.getInstance().get(JabRefPreferences.DEFAULT_LABEL_PATTERN));
    }

    /**
     * Required for LabelPatternUtilTest
     *
     * @param db the DB to use as global database
     */
    public static void setDataBase(BibDatabase db) {
        database = db;
    }

    private static String normalize(String content) {
        List<String> tokens = new ArrayList<>();
        int b = 0;
        StringBuilder and = new StringBuilder();
        StringBuilder token = new StringBuilder();
        for (int p = 0; p < content.length(); p++) {
            if (b == 0) {
                String andString = and.toString(); // Avoid lots of calls
                if (((andString.isEmpty()) && (content.charAt(p) == ' '))
                        || (" ".equals(andString) && (content.charAt(p) == 'a'))
                        || (" a".equals(andString) && (content.charAt(p) == 'n'))
                        || (" an".equals(andString) && (content.charAt(p) == 'd'))) {
                    and.append(content.charAt(p));
                } else if (" and".equals(and.toString()) && (content.charAt(p) == ' ')) {
                    and = new StringBuilder();
                    tokens.add(token.toString().trim());
                    token = new StringBuilder();
                } else {
                    if (content.charAt(p) == '{') {
                        b++;
                    }
                    if (content.charAt(p) == '}') {
                        b--;
                    }
                    token.append(and);
                    and = new StringBuilder();
                    token.append(content.charAt(p));
                }
            } else {
                token.append(content.charAt(p));
            }
        }
        tokens.add(token.toString());
        StringBuilder normalized = new StringBuilder("");

        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                normalized.append(" and ");
            }

            normalized.append(isInstitution(tokens.get(i)) ? generateInstitutionKey(tokens.get(i)) : removeDiacritics(
                    tokens.get(i)));
        }
        return normalized.toString();
    }

    /**
     * Will remove diacritics from the content.
     *
     * Replaces umlaut: \"x with xe, e.g. \"o -> oe, \"u -> ue, etc.
     * Removes all other diacritics: \?x -> x, e.g. \'a -> a, etc.
     *
     * @param content The content.
     * @return The content without diacritics.
     */
    private static String removeDiacritics(String content) {
        if (content.isEmpty()) {
            return content;
        }

        String result = content;
        // Replace umlaut with '?e'
        result = result.replaceAll("\\{\\\\\"([a-zA-Z])\\}", "$1e");
        result = result.replaceAll("\\\\\"\\{([a-zA-Z])\\}", "$1e");
        result = result.replaceAll("\\\\\"([a-zA-Z])", "$1e");
        // Remove diacritics
        result = result.replaceAll("\\{\\\\.([a-zA-Z])\\}", "$1");
        result = result.replaceAll("\\\\.\\{([a-zA-Z])\\}", "$1");
        result = result.replaceAll("\\\\.([a-zA-Z])", "$1");
        return result;
    }

    /**
     * Unifies umlauts.
     *
     * Replaces: $\ddot{\mathrm{X}}$ (an alternative umlaut) with: {\"X}
     * Replaces: \?{X} and \?X with {\?X}, where ? is a diacritic symbol
     *
     * @param content The content.
     * @return The content with unified diacritics.
     */
    private static String unifyDiacritics(String content) {
        return content.replaceAll(
                "\\$\\\\ddot\\{\\\\mathrm\\{([^\\}])\\}\\}\\$",
                "{\\\"$1}").replaceAll(
                "(\\\\[^\\-a-zA-Z])\\{?([a-zA-Z])\\}?",
                "{$1$2}");
    }

    /**
     * Check if a value is institution.
     *
     * This is usable for distinguishing between persons and institutions in
     * the author or editor fields.
     *
     * A person:
     *   - "John Doe"
     *   - "Doe, John"
     *
     * An institution:
     *   - "{The Big Company or Institution Inc.}"
     *   - "{The Big Company or Institution Inc. (BCI)}"
     *
     * @param author Author or editor.
     * @return True if the author or editor is an institution.
     */
    private static boolean isInstitution(String author) {
        return StringUtil.isInCurlyBrackets(author);
    }

    /**
     * <p>
     * An author or editor may be and institution not a person. In that case the
     * key generator builds very long keys, e.g.: for &ldquo;The Attributed
     * Graph Grammar System (AGG)&rdquo; ->
     * &ldquo;TheAttributedGraphGrammarSystemAGG&rdquo;.
     * </p>
     *
     * <p>
     * An institution name should be inside <code>{}</code> brackets. If the
     * institution name also includes its abbreviation this abbreviation should
     * be also in <code>{}</code> brackets. For the previous example the value
     * should look like:
     * <code>{The Attributed Graph Grammar System ({AGG})}</code>.
     * </p>
     *
     * <p>
     * If an institution includes its abbreviation, i.e. "...({XYZ})", first
     * such abbreviation should be used as the key value part of such author.
     * </p>
     *
     * <p>
     * If an institution does not include its abbreviation the key should be
     * generated form its name in the following way:
     * </p>
     *
     * <p>
     * The institution value can contain: institution name, part of the
     * institution, address, etc. Those information should be separated by
     * comma. Name of the institution and possible part of the institution
     * should be on the beginning, while address and secondary information
     * should be on the end.
     * </p>
     *
     * Each part is examined separately:
     * <ol>
     * <li>We remove all tokens of a part which are one of the defined ignore
     * words (the, press), which end with a dot (ltd., co., ...) and which first
     * character is lowercase (of, on, di, ...).</li>
     * <li>We detect a type of the part: university, technology institute,
     * department, school, rest
     * <ul>
     * <li>University: <code>"Uni[NameOfTheUniversity]"</code></li>
     * <li>Department: will be an abbreviation of all words beginning with the
     * uppercase letter except of words: <code>d[ei]p.*</code>, school,
     * faculty</li>
     * <li>School: same as department</li>
     * <li>Rest: If there are less than 3 tokens in such part than the result
     * will be by concatenating those tokens, otherwise the result will be build
     * from the first letters of words starting with and uppercase letter.</li>
     * </ul>
     * </ol>
     *
     * Parts are concatenated together in the following way:
     * <ul>
     * <li>If there is a university part use it otherwise use the rest part.</li>
     * <li>If there is a school part append it.</li>
     * <li>If there is a department part and it is not same as school part
     * append it.</li>
     * </ul>
     *
     * Rest part is only the first part which do not match any other type. All
     * other parts (address, ...) are ignored.
     *
     * @param content the institution to generate a Bibtex key for
     * @return <ul>
     *         <li>the institution key</li>
     *         <li>"" in the case of a failure</li>
     *         <li>null if content is null</li>
     *         </ul>
     */
    private static String generateInstitutionKey(String content) {
        if (content.isEmpty()) {
            return content;
        }

        String result = content;
        result = unifyDiacritics(result);
        result = result.replaceAll("^\\{", "").replaceAll("\\}$", "");
        Matcher matcher = REGEX_PATTERN.matcher(result);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        result = removeDiacritics(result);
        String[] parts = result.split(",");

        // Key parts
        String university = null;
        String department = null;
        String school = null;
        String rest = null;

        List<String> ignore = Arrays.asList("press", "the");
        for (int index = 0; index < parts.length; index++) {
            List<String> part = new ArrayList<>();

            // Cleanup: remove unnecessary words.
            for (String k : parts[index].replaceAll("\\{[A-Z]+\\}", "").split("[ \\-_]")) {
                if ((!(k.isEmpty()) // remove empty
                        && !ignore.contains(k.toLowerCase()) // remove ignored words
                        && (k.charAt(k.length() - 1) != '.')
                        && (String.valueOf(k.charAt(0))).matches("[A-Z]"))
                        || ((k.length() >= 3) && "uni".equalsIgnoreCase(k.substring(0, 2)))) {
                    part.add(k);
                }
            }

            boolean isUniversity = false; // university
            boolean isTechnology = false; // technology institute
            boolean isDepartment = false; // departments
            boolean isSchool = false; // schools

            // Deciding about a part type...
            for (String k : part) {
                if (k.matches("^[Uu][Nn][Ii].*")) { // Starts with "uni" case and locale independent
                    isUniversity = true;
                }
                if (k.matches("^[Tt][Ee][Cc][Hh].*")) { // Starts with "tech" case and locale independent
                    isTechnology = true;
                }
                if ("school".equalsIgnoreCase(k)) {
                    isSchool = true;
                }
                if (k.matches("^[Dd][EeIi][Pp].*") || k.matches("^[Ll][Aa][Bb].*")) { // Starts with "dep"/"dip"/"lab", case and locale independent
                    isDepartment = true;
                }
            }
            if (isTechnology) {
                isUniversity = false; // technology institute isn't university :-)
            }

            // University part looks like: Uni[NameOfTheUniversity]
            //
            // If university is detected than the previous part is suggested
            // as department
            if (isUniversity) {
                StringBuilder universitySB = new StringBuilder();
                universitySB.append("Uni");
                for (String k : part) {
                    if (!k.matches("^[Uu][Nn][Ii].*")) {
                        universitySB.append(k);
                    }
                }
                university = universitySB.toString();
                if ((index > 0) && (department == null)) {
                    department = parts[index - 1];
                }

                // School is an abbreviation of all the words beginning with a
                // capital letter excluding: department, school and faculty words.
                //
                // Explicitly defined department part is build the same way as
                // school
            } else if (isSchool || isDepartment) {
                StringBuilder schoolSB = new StringBuilder();
                StringBuilder departmentSB = new StringBuilder();
                for (String k : part) {
                    if (!k.matches("^[Dd][EeIi][Pp].*") && !"school".equalsIgnoreCase(k)
                            && !"faculty".equalsIgnoreCase(k)
                            && !(k.replaceAll(STARTING_CAPITAL_PATTERN, "").isEmpty())) {
                        if (isSchool) {
                            schoolSB.append(k.replaceAll(STARTING_CAPITAL_PATTERN, ""));
                        }
                        if (isDepartment) {
                            departmentSB.append(k.replaceAll(STARTING_CAPITAL_PATTERN, ""));
                        }
                    }
                }
                if (isSchool) {
                    school = schoolSB.toString();
                }
                if (isDepartment) {
                    department = departmentSB.toString();
                }
                // A part not matching university, department nor school.
            } else if (rest == null) {
                StringBuilder restSB = new StringBuilder();
                // Less than 3 parts -> concatenate those
                if (part.size() < 3) {
                    for (String k : part) {
                        restSB.append(k);
                    // More than 3 parts -> use 1st letter abbreviation
                    }
                } else {
                    for (String k : part) {
                        k = k.replaceAll(STARTING_CAPITAL_PATTERN, "");
                        if (!(k.isEmpty())) {
                            restSB.append(k);
                        }
                    }
                }
                rest = restSB.toString();
            }
        }

        // Putting parts together.
        return (university == null ? rest : university)
                + (school == null ? "" : school)
                + ((department == null)
                || ((school != null) && department.equals(school)) ?
                        "" : department);
    }

    /**
     * This method takes a string of the form [field1]spacer[field2]spacer[field3]...,
     * where the fields are the (required) fields of a BibTex entry. The string is split
     * into fields and spacers by recognizing the [ and ].
     *
     * @param labelPattern a <code>String</code>
     * @return an <code>ArrayList</code> The first item of the list
     * is a string representation of the key pattern (the parameter),
     * the remaining items are the fields
     */
    public static List<String> split(String labelPattern) {
        // A holder for fields of the entry to be used for the key
        List<String> fieldList = new ArrayList<>();

        // Before we do anything, we add the parameter to the ArrayLIst
        fieldList.add(labelPattern);

        StringTokenizer tok = new StringTokenizer(labelPattern, "[]", true);
        while (tok.hasMoreTokens()) {
            fieldList.add(tok.nextToken());
        }
        return fieldList;
    }

    /**
     * Generates a BibTeX label according to the pattern for a given entry type, and saves the unique label in the
     * <code>Bibtexentry</code>.
     *
     * The given database is used to avoid duplicate keys.
     *
     * @param dBase a <code>BibDatabase</code>
     * @param entry a <code>BibEntry</code>
     * @return modified BibEntry
     */
    public static void makeLabel(MetaData metaData, BibDatabase dBase, BibEntry entry) {
        database = dBase;
        String key;
        StringBuilder stringBuilder = new StringBuilder();
        boolean forceUpper = false;
        boolean forceLower = false;

        try {
            // get the type of entry
            String entryType = entry.getType();
            // Get the arrayList corresponding to the type
            List<String> typeList = new ArrayList<>(metaData.getLabelPattern().getValue(entryType));
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
                    String[] parts = parseFieldMarker(typeListEntry);

                    String label = makeLabel(entry, parts[0]);

                    // apply modifier if present
                    if (parts.length > 1) {
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
        key = checkLegalKey(stringBuilder.toString());

        // Remove Regular Expressions while generating Keys
        String regex = Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REGEX);
        if ((regex != null) && !regex.trim().isEmpty()) {
            String replacement = Globals.prefs.get(JabRefPreferences.KEY_PATTERN_REPLACEMENT);
            key = key.replaceAll(regex, replacement);
        }

        if (forceUpper) {
            key = key.toUpperCase();
        }
        if (forceLower) {
            key = key.toLowerCase();
        }

        String oldKey = entry.getCiteKey();
        int occurrences = database.getNumberOfKeyOccurrences(key);

        if ((oldKey != null) && oldKey.equals(key)) {
            occurrences--; // No change, so we can accept one dupe.
        }

        boolean alwaysAddLetter = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER);
        boolean firstLetterA = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A);

        if (!alwaysAddLetter && (occurrences == 0)) {
            // No dupes found, so we can just go ahead.
            if (!key.equals(oldKey)) {
                if (!database.containsEntryWithId(entry.getId())) {
                    // entry does not (yet) exist in the database, just update the entry
                    entry.setField(BibEntry.KEY_FIELD, key);
                } else {
                    database.setCiteKeyForEntry(entry, key);
                }
            }

        } else {
            // The key is already in use, so we must modify it.
            int number = 0;
            if (!alwaysAddLetter && !firstLetterA) {
                number = 1;
            }

            String moddedKey = key + getAddition(number);
            occurrences = database.getNumberOfKeyOccurrences(moddedKey);

            if ((oldKey != null) && oldKey.equals(moddedKey)) {
                occurrences--;
            }

            while (occurrences > 0) {
                number++;
                moddedKey = key + getAddition(number);

                occurrences = database.getNumberOfKeyOccurrences(moddedKey);
                if ((oldKey != null) && oldKey.equals(moddedKey)) {
                    occurrences--;
                }
            }

            if (!moddedKey.equals(oldKey)) {
                if (!database.containsEntryWithId(entry.getId())) {
                    // entry does not (yet) exist in the database, just update the entry
                    entry.setField(BibEntry.KEY_FIELD, moddedKey);
                } else {
                    database.setCiteKeyForEntry(entry, moddedKey);
                }
            }
        }
    }

    /**
     * Applies modifiers to a label generated based on a field marker.
     * @param label The generated label.
     * @param parts String array containing the modifiers.
     * @param offset The number of initial items in the modifiers array to skip.
     * @return The modified label.
     */
    public static String applyModifiers(String label, String[] parts, int offset) {
        String resultingLabel = label;
        if (parts.length > offset) {
            for (int j = offset; j < parts.length; j++) {
                String modifier = parts[j];

                if ("lower".equals(modifier)) {
                    resultingLabel = resultingLabel.toLowerCase();
                } else if ("upper".equals(modifier)) {
                    resultingLabel = resultingLabel.toUpperCase();
                } else if ("abbr".equals(modifier)) {
                    // Abbreviate - that is,
                    StringBuilder abbreviateSB = new StringBuilder();
                    String[] words = resultingLabel.replaceAll("[\\{\\}']", "")
                            .split("[\\(\\) \r\n\"]");
                    for (String word1 : words) {
                        if (!word1.isEmpty()) {
                            abbreviateSB.append(word1.charAt(0));
                        }
                    }
                    resultingLabel = abbreviateSB.toString();

                } else if (!modifier.isEmpty() && (modifier.charAt(0) == '(') && modifier.endsWith(")")) {
                    // Alternate text modifier in parentheses. Should be inserted if
                    // the label is empty:
                    if (resultingLabel.isEmpty() && (modifier.length() > 2)) {
                        return modifier.substring(1, modifier.length() - 1);
                    }

                } else {
                    LOGGER.info("Key generator warning: unknown modifier '"
                            + modifier + "'.");
                }
            }
        }
        return resultingLabel;
    }

    public static String makeLabel(BibEntry entry, String value) {
        String val = value;
        try {
            if (val.startsWith("auth") || val.startsWith("pureauth")) {

                /*
                 * For label code "auth...": if there is no author, but there
                 * are editor(s) (e.g. for an Edited Book), use the editor(s)
                 * instead. (saw27@mrao.cam.ac.uk). This is what most people
                 * want, but in case somebody really needs a field which expands
                 * to nothing if there is no author (e.g. someone who uses both
                 * "auth" and "ed" in the same label), we provide an alternative
                 * form "pureauth..." which does not do this fallback
                 * substitution of editor.
                 */
                String authString = entry.getField("author");
                if (authString != null) {
                    authString = normalize(database.resolveForStrings(authString));
                }

                if (val.startsWith("pure")) {
                    // remove the "pure" prefix so the remaining
                    // code in this section functions correctly
                    val = val.substring(4);
                }

                if ((authString == null) || authString.isEmpty()) {
                    authString = entry.getField("editor");
                    if (authString == null) {
                        authString = "";
                    } else {
                        authString = normalize(database.resolveForStrings(authString));
                    }
                }

                // Gather all author-related checks, so we don't
                // have to check all the time.
                if ("auth".equals(val)) {
                    return firstAuthor(authString);
                } else if ("authForeIni".equals(val)) {
                    return firstAuthorForenameInitials(authString);
                } else if ("authFirstFull".equals(val)) {
                    return firstAuthorVonAndLast(authString);
                } else if ("authors".equals(val)) {
                    return allAuthors(authString);
                } else if ("authorsAlpha".equals(val)) {
                    return authorsAlpha(authString);
                }
                // Last author's last name
                else if ("authorLast".equals(val)) {
                    return lastAuthor(authString);
                } else if ("authorLastForeIni".equals(val)) {
                    return lastAuthorForenameInitials(authString);
                } else if ("authorIni".equals(val)) {
                    return oneAuthorPlusIni(authString);
                } else if (val.matches("authIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    String s = authIniN(authString, num);
                    return s == null ? "" : s;
                } else if ("auth.auth.ea".equals(val)) {
                    String s = authAuthEa(authString);
                    return s == null ? "" : s;
                } else if ("auth.etal".equals(val)) {
                    String s = authEtal(authString, ".", ".etal");
                    return s == null ? "" : s;
                } else if ("authEtAl".equals(val)) {
                    String s = authEtal(authString, "", "EtAl");
                    return s == null ? "" : s;
                } else if ("authshort".equals(val)) {
                    String s = authshort(authString);
                    return s == null ? "" : s;
                } else if (val.matches("auth[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    String s = authNofMth(authString, Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]));
                    return s == null ? "" : s;
                } else if (val.matches("auth\\d+")) {
                    // authN. First N chars of the first author's last
                    // name.

                    String fa = firstAuthor(authString);
                    if (fa == null) {
                        return "";
                    }
                    int num = Integer.parseInt(val.substring(4));
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else if (val.matches("authors\\d+")) {
                    String s = nAuthors(authString, Integer.parseInt(val.substring(7)));
                    return s == null ? "" : s;
                } else {
                    // This "auth" business was a dead end, so just
                    // use it literally:
                    return getField(entry, val);
                }
            } else if (val.startsWith("ed")) {
                // Gather all markers starting with "ed" here, so we
                // don't have to check all the time.
                if ("edtr".equals(val)) {
                    return firstAuthor(entry.getField("editor"));
                } else if ("edtrForeIni".equals(val)) {
                    return firstAuthorForenameInitials(entry.getField("editor"));
                } else if ("editors".equals(val)) {
                    return allAuthors(entry.getField("editor"));
                    // Last author's last name
                } else if ("editorLast".equals(val)) {
                    return lastAuthor(entry.getField("editor"));
                } else if ("editorLastForeIni".equals(val)) {
                    return lastAuthorForenameInitials(entry.getField("editor"));
                } else if ("editorIni".equals(val)) {
                    return oneAuthorPlusIni(entry.getField("editor"));
                } else if (val.matches("edtrIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    String s = authIniN(entry.getField("editor"), num);
                    return s == null ? "" : s;
                } else if (val.matches("edtr[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    String s = authNofMth(entry.getField("editor"),
                            Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]) - 1);
                    return s == null ? "" : s;
                } else if ("edtr.edtr.ea".equals(val)) {
                    String s = authAuthEa(entry.getField("editor"));
                    return s == null ? "" : s;
                } else if ("edtrshort".equals(val)) {
                    String s = authshort(entry.getField("editor"));
                    return s == null ? "" : s;
                }
                // authN. First N chars of the first author's last
                // name.
                else if (val.matches("edtr\\d+")) {
                    String fa = firstAuthor(entry.getField("editor"));
                    if (fa == null) {
                        return "";
                    }
                    int num = Integer.parseInt(val.substring(4));
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else {
                    // This "ed" business was a dead end, so just
                    // use it literally:
                    return getField(entry, val);
                }
            } else if ("firstpage".equals(val)) {
                return firstPage(entry.getField("pages"));
            } else if ("lastpage".equals(val)) {
                return lastPage(entry.getField("pages"));
            } else if ("shorttitle".equals(val)) {
                return getTitleWords(3, entry.getField("title"));
            } else if ("veryshorttitle".equals(val)) {
                return getTitleWords(1, entry.getField("title"));
            } else if ("shortyear".equals(val)) {
                String ss = entry.getFieldOrAlias("year");
                if (ss == null) {
                    return "";
                } else if (ss.startsWith("in") || ss.startsWith("sub")) {
                    return "IP";
                } else if (ss.length() > 2) {
                    return ss.substring(ss.length() - 2);
                } else {
                    return ss;
                }
            } else if (val.matches("keyword\\d+")) {
                // according to LabelPattern.php, it returns keyword number n
                int num = Integer.parseInt(val.substring(7));
                List<String> separatedKeywords = entry.getSeparatedKeywords();
                if (separatedKeywords.size() < num) {
                    // not enough keywords
                    return "";
                } else {
                    // num counts from 1 to n, but index in arrayList count from 0 to n-1
                    return separatedKeywords.get(num-1);
                }
            } else if (val.matches("keywords\\d*")) {
                // return all keywords, not separated
                int num;
                if (val.length() > 8) {
                    num = Integer.parseInt(val.substring(8));
                } else {
                    num = Integer.MAX_VALUE;
                }
                List<String> separatedKeywords = entry.getSeparatedKeywords();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < Math.min(separatedKeywords.size(), num); i++) {
                    String keyword = separatedKeywords.get(i);
                    // remove all spaces
                    keyword = keyword.replaceAll("\\s+", "");
                    sb.append(keyword);
                }
                return sb.toString();
            } else {
                // we haven't seen any special demands
                return getField(entry, val);
            }
        } catch (NullPointerException ex) {
            LOGGER.debug("Problem making label", ex);
            return "";
        }

    }

    /**
     * Look up a field of a BibEntry, returning its String value, or an
     * empty string if it isn't set.
     * @param entry The entry.
     * @param field The field to look up.
     * @return The field value.
     */
    private static String getField(BibEntry entry, String field) {
        String s = entry.getFieldOrAlias(field);
        return s == null ? "" : s;
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
     * Determines "number" words out of the "title" field in the given BibTeX entry
     */
    public static String getTitleWords(int number, String title) {
        String ss = new RemoveLatexCommands().format(title);
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder current;
        int piv = 0;
        int words = 0;

        // sorry for being English-centric. I guess these
        // words should really be an editable preference.
        mainl: while ((piv < ss.length()) && (words < number)) {
            current = new StringBuilder();
            // Get the next word:
            while ((piv < ss.length()) && !Character.isWhitespace(ss.charAt(piv))
                    && (ss.charAt(piv) != '-')) {
                current.append(ss.charAt(piv));
                piv++;
            }
            piv++;
            // Check if it is ok:
            String word = current.toString().trim();
            if (word.isEmpty()) {
                continue;
            }
            for (String smallWord: Word.SMALLER_WORDS) {
                if (word.equalsIgnoreCase(smallWord)) {
                    continue mainl;
                }
            }

            // If we get here, the word was accepted.
            if (stringBuilder.length() > 0) {
                stringBuilder.append(' ');
            }
            stringBuilder.append(word);
            words++;
        }

        return keepLettersAndDigitsOnly(stringBuilder.toString());
    }

    private static String keepLettersAndDigitsOnly(String in) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            if (Character.isLetterOrDigit(in.charAt(i))) {
                stringBuilder.append(in.charAt(i));
            }
        }
        return stringBuilder.toString();
    }


    /**
     * Gets the last name of the first author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the surname of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthor(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }
        String s = authorList.getAuthor(0).getLast();
        return s == null ? "" : s;

    }

    /**
     * Gets the first name initials of the first author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the first name initial of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthorForenameInitials(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }
        String s = authorList.getAuthor(0).getFirstAbbr();
        return s == null ? "" : s.substring(0, 1);
    }

    /**
     * Gets the von part and the last name of the first author/editor
     * No spaces are returned
     *
     * @param authorField
     *            a <code>String</code>
     * @return the von part and surname of an author/editor or "" if no author was found.
     *  This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String firstAuthorVonAndLast(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }
        String vonAuthor = authorList.getAuthor(0).getVon().replaceAll(" ", "");
        StringBuilder stringBuilder = new StringBuilder();
        if (vonAuthor != null) {
            stringBuilder.append(vonAuthor);
        }
        vonAuthor = authorList.getAuthor(0).getLast();
        if (vonAuthor != null) {
            stringBuilder.append(vonAuthor);
        }
        return stringBuilder.toString();
    }

    /**
     * Gets the last name of the last author/editor
     * @param authorField a <code>String</code>
     * @return the surname of an author/editor
     */
    public static String lastAuthor(String authorField) {
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\s+\\band\\b\\s+");
        if (tokens.length > 0) {
            String[] lastAuthor = tokens[tokens.length - 1].split(",");
            return lastAuthor[0];
        } else {
            // if author is empty
            return "";
        }
    }

    /**
     * Gets the forename initials of the last author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the forename initial of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    public static String lastAuthorForenameInitials(String authorField) {
        AuthorList authorList = AuthorList.parse(authorField);
        if (authorList.isEmpty()) {
            return "";
        }
        String s = authorList.getAuthor(authorList.getNumberOfAuthors() - 1).getFirstAbbr();
        return s == null ? "" : s.substring(0, 1);
    }

    /**
     * Gets the last name of all authors/editors
     * @param authorField a <code>String</code>
     * @return the sur name of all authors/editors
     */
    public static String allAuthors(String authorField) {
        // Quick hack to use NAuthors to avoid code duplication
        return nAuthors(authorField, Integer.MAX_VALUE);
    }

    /**
     * Returns the authors according to the BibTeX-alpha-Style
     * @param authorField string containing the value of the author field
     * @return the initials of all authornames
     */
    public static String authorsAlpha(String authorField) {
        String authors = "";

        String fixedAuthors = AuthorList.fixAuthorLastNameOnlyCommas(authorField, false);

        // drop the "and" before the last author
        // -> makes processing easier
        fixedAuthors = fixedAuthors.replace(" and ", ", ");

        String[] tokens = fixedAuthors.split(",");
        int max = tokens.length > 4 ? 3 : tokens.length;
        if (max == 1) {
            String[] firstAuthor = tokens[0].replaceAll("\\s+", " ").trim().split(" ");
            // take first letter of any "prefixes" (e.g. van der Aalst -> vd)
            for (int j = 0; j < (firstAuthor.length - 1); j++) {
                authors = authors.concat(firstAuthor[j].substring(0, 1));
            }
            // append last part of last name completely
            authors = authors.concat(firstAuthor[firstAuthor.length - 1].substring(0,
                    Math.min(3, firstAuthor[firstAuthor.length - 1].length())));
        } else {
            for (int i = 0; i < max; i++) {
                // replace all whitespaces by " "
                // split the lastname at " "
                String[] curAuthor = tokens[i].replaceAll("\\s+", " ").trim().split(" ");
                for (String aCurAuthor : curAuthor) {
                    // use first character of each part of lastname
                    authors = authors.concat(aCurAuthor.substring(0, 1));
                }
            }
            if (tokens.length > 4) {
                authors = authors.concat("+");
            }
        }
        return authors;
    }

    /**
     * Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     * @param authorField a <code>String</code>
     * @param n the number of desired authors
     * @return Gets the surnames of the first N authors and appends EtAl if there are more than N authors
     */
    public static String nAuthors(String authorField, int n) {
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\s+\\band\\b\\s+");
        int i = 0;
        StringBuilder authorSB = new StringBuilder();
        while ((tokens.length > i) && (i < n)) {
            String lastName = tokens[i].replaceAll(",\\s+.*", "");
            authorSB.append(lastName);
            i++;
        }
        if (tokens.length > n) {
            authorSB.append("EtAl");
        }
        return authorSB.toString();
    }

    /**
     * Gets the first part of the last name of the first
     * author/editor, and appends the last name initial of the
     * remaining authors/editors.
     * Maximum 5 characters
     * @param authorField a <code>String</code>
     * @return the surname of all authors/editors
     */
    public static String oneAuthorPlusIni(String authorField) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);
        String[] tokens = fixedAuthorField.split("\\s+\\band\\b\\s+");
        if (tokens.length == 0) {
            return "";
        }

        String firstAuthor = tokens[0].split(",")[0];
        StringBuilder authorSB = new StringBuilder();
        authorSB.append(firstAuthor.substring(0, Math.min(CHARS_OF_FIRST, firstAuthor.length())));
        int i = 1;
        while (tokens.length > i) {
            // convert lastname, firstname to firstname lastname
            authorSB.append(tokens[i].charAt(0));
            i++;
        }
        return authorSB.toString();
    }

    /**
     * auth.auth.ea format:
     * Isaac Newton and James Maxwell and Albert Einstein (1960)
     * Isaac Newton and James Maxwell (1960)
     *  give:
     * Newton.Maxwell.ea
     * Newton.Maxwell
     */
    public static String authAuthEa(String authorField) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = fixedAuthorField.split("\\s+\\band\\b\\s+");
        if (tokens.length == 0) {
            return "";
        }

        StringBuilder author = new StringBuilder();
        // append first author
        author.append((tokens[0].split(","))[0]);
        if (tokens.length >= 2) {
            // append second author
            author.append('.').append((tokens[1].split(","))[0]);
        }
        if (tokens.length > 2) {
            // append ".ea" if more than 2 authors
            author.append(".ea");
        }

        return author.toString();
    }

    /**
     * auth.etal, authEtAl, ... format:
     * Isaac Newton and James Maxwell and Albert Einstein (1960)
     * Isaac Newton and James Maxwell (1960)
     *
     *  auth.etal give (delim=".", append=".etal"):
     * Newton.etal
     * Newton.Maxwell
     *
     *  authEtAl give (delim="", append="EtAl"):
     * NewtonEtAl
     * NewtonMaxwell
     *
     * Note that [authEtAl] equals [authors2]
     */
    public static String authEtal(String authorField, String delim,
            String append) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = fixedAuthorField.split("\\s*\\band\\b\\s*");
        if (tokens.length == 0) {
            return "";
        }
        StringBuilder author = new StringBuilder();
        author.append((tokens[0].split(","))[0]);
        if (tokens.length == 2) {
            author.append(delim).append((tokens[1].split(","))[0]);
        } else if (tokens.length > 2) {
            author.append(append);
        }

        return author.toString();
    }

    /**
     * The first N characters of the Mth author/editor.
     * M starts counting from 1
     */
    public static String authNofMth(String authorField, int n, int m) {
        // have m counting from 0
        int mminusone = m - 1;

        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = fixedAuthorField.split("\\s+\\band\\b\\s+");
        if ((tokens.length <= mminusone) || (n < 0) || (mminusone < 0)) {
            return "";
        }
        String lastName = (tokens[mminusone].split(","))[0];
        if (lastName.length() <= n) {
            return lastName;
        } else {
            return lastName.substring(0, n);
        }
    }

    /**
     * authshort format:
     * added by Kolja Brix, kbx@users.sourceforge.net
     *
     * given author names
     *
     *   Isaac Newton and James Maxwell and Albert Einstein and N. Bohr
     *
     *   Isaac Newton and James Maxwell and Albert Einstein
     *
     *   Isaac Newton and James Maxwell
     *
     *   Isaac Newton
     *
     * yield
     *
     *   NME+
     *
     *   NME
     *
     *   NM
     *
     *   Newton
     */
    public static String authshort(String authorField) {
        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();
        String[] tokens = fixedAuthorField.split("\\band\\b");
        int i = 0;

        if (tokens.length == 1) {
            author.append(authNofMth(fixedAuthorField, fixedAuthorField.length(), 1));
        } else if (tokens.length >= 2) {
            while ((tokens.length > i) && (i < 3)) {
                author.append(authNofMth(fixedAuthorField, 1, i + 1));
                i++;
            }
            if (tokens.length > 3) {
                author.append('+');
            }
        }

        return author.toString();
    }

    /**
     * authIniN format:
     *
     * Each author gets (N div #authors) chars, the remaining (N mod #authors)
     * chars are equally distributed to the authors first in the row.
     *
     * If (N < #authors), only the first N authors get mentioned.
     *
     * For example if
     *
     * a) I. Newton and J. Maxwell and A. Einstein and N. Bohr (..)
     *
     * b) I. Newton and J. Maxwell and A. Einstein
     *
     * c) I. Newton and J. Maxwell
     *
     * d) I. Newton
     *
     * authIni4 gives: a) NMEB, b) NeME, c) NeMa, d) Newt
     *
     * @param authorField
     *            The authors to format.
     *
     * @param n
     *            The maximum number of characters this string will be long. A
     *            negative number or zero will lead to "" be returned.
     *
     * @throws NullPointerException
     *             if authorField is null and n > 0
     */
    public static String authIniN(String authorField, int n) {

        if (n <= 0) {
            return "";
        }

        String fixedAuthorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();
        String[] tokens = fixedAuthorField.split("\\band\\b");

        if (tokens.length == 0) {
            return author.toString();
        }

        int i = 0;
        int charsAll = n / tokens.length;
        while (tokens.length > i) {
            if (i < (n % tokens.length)) {
                author.append(authNofMth(fixedAuthorField, charsAll + 1, i + 1));
            } else {
                author.append(authNofMth(fixedAuthorField, charsAll, i + 1));
            }
            i++;
        }

        if (author.length() <= n) {
            return author.toString();
        } else {
            return author.toString().substring(0, n);
        }
    }

    /**
     * Split the pages field into separate numbers and return the lowest
     *
     * @param pages
     *            (may not be null) a pages string such as 42--111 or
     *            7,41,73--97 or 43+
     *
     * @return the first page number or "" if no number is found in the string
     *
     * @throws NullPointerException
     *             if pages is null
     */
    public static String firstPage(String pages) {
        final String[] splitPages = pages.split("\\D+");
        int result = Integer.MAX_VALUE;
        for (String n : splitPages) {
            if (n.matches("\\d+")) {
                result = Math.min(Integer.parseInt(n), result);
            }
        }

        if (result == Integer.MAX_VALUE) {
            return "";
        } else {
            return String.valueOf(result);
        }
    }

    /**
     * Split the pages field into separate numbers and return the highest
     *
     * @param pages
     *            a pages string such as 42--111 or 7,41,73--97 or 43+
     *
     * @return the first page number or "" if no number is found in the string
     *
     * @throws NullPointerException
     *             if pages is null.
     */
    public static String lastPage(String pages) {
        final String[] splitPages = pages.split("\\D+");
        int result = Integer.MIN_VALUE;
        for (String n : splitPages) {
            if (n.matches("\\d+")) {
                result = Math.max(Integer.parseInt(n), result);
            }
        }

        if (result == Integer.MIN_VALUE) {
            return "";
        } else {
            return String.valueOf(result);
        }
    }

    /**
     * Parse a field marker with modifiers, possibly containing a parenthesised modifier,
     * as well as escaped colons and parentheses.
     * @param arg The argument string.
     * @return An array of strings representing the parts of the marker
     */
    private static String[] parseFieldMarker(String arg) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        int inParenthesis = 0;
        for (int i = 0; i < arg.length(); i++) {
            if ((arg.charAt(i) == ':') && !escaped && (inParenthesis == 0)) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else if ((arg.charAt(i) == '(') && !escaped) {
                inParenthesis++;
                current.append(arg.charAt(i));
            } else if ((arg.charAt(i) == ')') && !escaped && (inParenthesis > 0)) {
                inParenthesis--;
                current.append(arg.charAt(i));
            } else if (arg.charAt(i) == '\\') {
                if (escaped) {
                    escaped = false;
                    current.append(arg.charAt(i));
                } else {
                    escaped = true;
                }
            } else if (escaped) {
                current.append(arg.charAt(i));
                escaped = false;
            } else {
                current.append(arg.charAt(i));
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[parts.size()]);
    }

    /**
     * This method returns a String similar to the one passed in, except that it is molded into a form that is
     * acceptable for bibtex.
     * <p>
     * Watch-out that the returned string might be of length 0 afterwards.
     *
     * @param key mayBeNull
     */
    public static String checkLegalKey(String key) {
        if (key == null) {
            return null;
        }
        return checkLegalKey(key,
                JabRefPreferences.getInstance().getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
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

    public static List<String> getDefaultLabelPattern() {
        return defaultLabelPattern;
    }
}
