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
package net.sf.jabref.logic.labelPattern;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.formatter.casechanger.Word;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.exporter.layout.format.RemoveLatexCommands;
import net.sf.jabref.util.Util;

/**
 * This is the utility class of the LabelPattern package.
 */
public class LabelPatternUtil {

    // All single characters that we can use for extending a key to make it unique:
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";

    private static final Log LOGGER = LogFactory.getLog(LabelPatternUtil.class);

    public static ArrayList<String> DEFAULT_LABELPATTERN;

    static {
        LabelPatternUtil.updateDefaultPattern();
    }

    private static BibtexDatabase database;

    public static void updateDefaultPattern() {
        LabelPatternUtil.DEFAULT_LABELPATTERN = LabelPatternUtil.split(JabRefPreferences.getInstance().get(JabRefPreferences.DEFAULT_LABEL_PATTERN));
    }

    /**
     * Required fro LabelPatternUtilTest
     *
     * @param db the DB to use as global database
     */
    public static void setDataBase(BibtexDatabase db) {
        LabelPatternUtil.database = db;
    }

    private static String normalize(String content) {
        List<String> tokens = new ArrayList<>();
        int b = 0;
        String and = "";
        String token = "";
        for (int p = 0; p < content.length(); p++) {
            if (b == 0) {
                if ((and.equals("") && (content.charAt(p) == ' '))
                        || (and.equals(" ") && (content.charAt(p) == 'a'))
                        || (and.equals(" a") && (content.charAt(p) == 'n'))
                        || (and.equals(" an") && (content.charAt(p) == 'd'))) {
                    and += content.charAt(p);
                } else if (and.equals(" and") && (content.charAt(p) == ' ')) {
                    and = "";
                    tokens.add(token.trim());
                    token = "";
                } else {
                    if (content.charAt(p) == '{') {
                        b++;
                    }
                    if (content.charAt(p) == '}') {
                        b--;
                    }
                    token += and;
                    and = "";
                    token += content.charAt(p);
                }
            } else {
                token += content.charAt(p);
            }
        }
        tokens.add(token);
        StringBuilder normalized = new StringBuilder("");

        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                normalized.append(" and ");
            }

            normalized.append(LabelPatternUtil.isInstitution(tokens.get(i))
                    ? LabelPatternUtil.generateInstitutionKey(tokens.get(i))
                    : LabelPatternUtil.removeDiacritics(tokens.get(i)));
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
        if (content == null) {
            return null;
        }

        // Replace umaut with '?e'
        content = content.replaceAll("\\{\\\\\"([a-zA-Z])\\}", "$1e");
        content = content.replaceAll("\\\\\"\\{([a-zA-Z])\\}", "$1e");
        content = content.replaceAll("\\\\\"([a-zA-Z])", "$1e");
        // Remove diacritics
        content = content.replaceAll("\\{\\\\.([a-zA-Z])\\}", "$1");
        content = content.replaceAll("\\\\.\\{([a-zA-Z])\\}", "$1");
        content = content.replaceAll("\\\\.([a-zA-Z])", "$1");
        return content;
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
        if (content == null) {
            return null;
        }
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
        return (author.charAt(0) == '{')
                && (author.charAt(author.length() - 1) == '}');
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
     * uppercase letter except of words: <code>d[ei]part.*</code>, school,
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
     *         <li>the institutation key</li>
     *         <li>"" in the case of a failure</li>
     *         <li>null if content is null</li>
     *         </ul>
     */
    private static String generateInstitutionKey(String content) {
        if (content == null) {
            return null;
        }
        content = LabelPatternUtil.unifyDiacritics(content);
        List<String> ignore = Arrays.asList("press", "the");
        content = content.replaceAll("^\\{", "").replaceAll("\\}$", "");
        Pattern regex = Pattern.compile(".*\\(\\{([A-Z]+)\\}\\).*");
        Matcher matcher = regex.matcher(content);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        content = LabelPatternUtil.removeDiacritics(content);
        String[] parts = content.split(",");

        // Key parts
        String university = null;
        String department = null;
        String school = null;
        String rest = null;

        for (int index = 0; index < parts.length; index++) {
            List<String> part = new ArrayList<>();

            // Cleanup: remove unnecessary words.
            for (String k : parts[index].replaceAll("\\{[A-Z]+\\}", "").split("[ \\-_]")) {
                if ((!k.equals("") // remove empty
                        && !ignore.contains(k.toLowerCase()) // remove ignored words
                        && (k.charAt(k.length() - 1) != '.')
                        && (k.charAt(0) + "").matches("[A-Z]"))
                        || ((k.length() >= 3) && k.toLowerCase().substring(0, 2).equals("uni"))) {
                    part.add(k);
                }
            }

            boolean isUniversity = false; // university
            boolean isTechnology = false; // technology institute
            boolean isDepartment = false; // departments
            boolean isSchool = false; // schools

            // Deciding about a part type...
            for (String k : part) {
                if ((k.length() >= 5) && k.toLowerCase().substring(0, 5).equals("univ")) {
                    isUniversity = true;
                }
                if ((k.length() >= 6) && k.toLowerCase().substring(0, 6).equals("techn")) {
                    isTechnology = true;
                }
                if (k.toLowerCase().equals("school")) {
                    isSchool = true;
                }
                if (((k.length() >= 7) && k.toLowerCase().substring(0, 7).matches("d[ei]part"))
                        || ((k.length() >= 4) && k.toLowerCase().substring(0, 4).equals("lab"))) {
                    isDepartment = true;
                }
            }
            if (isTechnology)
             {
                isUniversity = false; // technology institute isn't university :-)
            }

            // University part looks like: Uni[NameOfTheUniversity]
            //
            // If university is detected than the previous part is suggested
            // as department
            if (isUniversity) {
                university = "Uni";
                for (String k : part) {
                    if ((k.length() >= 5) && !k.toLowerCase().substring(0, 5).equals("univ")) {
                        university += k;
                    }
                }
                if ((index > 0) && (department == null)) {
                    department = parts[index - 1];
                }

                // School is an abbreviation of all the words beginning with a
                // capital letter excluding: department, school and faculty words.
                //
                // Explicitly defined department part is build the same way as
                // school
            } else if (isSchool || isDepartment) {
                if (isSchool) {
                    school = "";
                }
                if (isDepartment) {
                    department = "";
                }

                for (String k : part) {
                    if ((k.length() >= 7) && !k.toLowerCase().substring(0, 7).matches("d[ei]part")
                            && !k.toLowerCase().equals("school")
                            && !k.toLowerCase().equals("faculty")
                            && !k.replaceAll("[^A-Z]", "").equals("")) {
                        if (isSchool) {
                            school += k.replaceAll("[^A-Z]", "");
                        }
                        if (isDepartment) {
                            department += k.replaceAll("[^A-Z]", "");
                        }
                    }
                }
                // A part not matching university, department nor school.
            } else if (rest == null) {
                rest = "";
                // Less than 3 parts -> concatenate those
                if (part.size() < 3) {
                    for (String k : part)
                     {
                        rest += k;
                    // More than 3 parts -> use 1st letter abbreviation
                    }
                } else {
                    for (String k : part) {
                        k = k.replaceAll("[^A-Z]", "");
                        if (!k.equals("")) {
                            rest += k;
                        }
                    }
                }
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
    public static ArrayList<String> split(String labelPattern) {
        // A holder for fields of the entry to be used for the key
        ArrayList<String> fieldList = new ArrayList<>();

        // Before we do anything, we add the parameter to the ArrayLIst
        fieldList.add(labelPattern);

        //String[] ss = labelPattern.split("\\[|\\]");
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
     * @param dBase a <code>BibtexDatabase</code>
     * @param entry a <code>BibtexEntry</code>
     * @return modified Bibtexentry
     */
    public static void makeLabel(MetaData metaData, BibtexDatabase dBase, BibtexEntry entry) {
        LabelPatternUtil.database = dBase;
        ArrayList<String> typeList;
        String key;
        StringBuilder stringBuilder = new StringBuilder();
        boolean forceUpper = false;
        boolean forceLower = false;

        try {
            // get the type of entry
            String entryType = entry.getType().getName().toLowerCase();
            // Get the arrayList corresponding to the type
            typeList = metaData.getLabelPattern().getValue(entryType);
            int typeListSize = typeList.size();
            boolean field = false;
            for (int i = 1; i < typeListSize; i++) {
                String typeListEntry = typeList.get(i);
                if (typeListEntry.equals("[")) {
                    field = true;
                } else if (typeListEntry.equals("]")) {
                    field = false;
                } else if (field) {
                    // check whether there is a modifier on the end such as
                    // ":lower"
                    // String modifier = null;
                    String[] parts = LabelPatternUtil.parseFieldMarker(typeListEntry);//val.split(":");

                    String label = LabelPatternUtil.makeLabel(entry, parts[0]);

                    // apply modifier if present
                    if (parts.length > 1) {
                        label = LabelPatternUtil.applyModifiers(label, parts, 1);
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
        key = Util.checkLegalKey(stringBuilder.toString());

        // Remove Regular Expressions while generating Keys
        String regex = Globals.prefs.get("KeyPatternRegex");
        if ((regex != null) && !regex.trim().isEmpty()) {
            String replacement = Globals.prefs.get("KeyPatternReplacement");
            key = key.replaceAll(regex, replacement);
        }

        if (forceUpper) {
            key = key.toUpperCase();
        }
        if (forceLower) {
            key = key.toLowerCase();
        }

        String oldKey = entry.getCiteKey();
        int occurrences = LabelPatternUtil.database.getNumberOfKeyOccurrences(key);

        if ((oldKey != null) && oldKey.equals(key))
         {
            occurrences--; // No change, so we can accept one dupe.
        }

        boolean alwaysAddLetter = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER);
        boolean firstLetterA = Globals.prefs.getBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A);

        if (!alwaysAddLetter && (occurrences == 0)) {
            // No dupes found, so we can just go ahead.
            if (!key.equals(oldKey)) {
                if (LabelPatternUtil.database.getEntryById(entry.getId()) == null) {
                    // entry does not (yet) exist in the database, just update the entry
                    entry.setField(BibtexEntry.KEY_FIELD, key);
                } else {
                    LabelPatternUtil.database.setCiteKeyForEntry(entry.getId(), key);
                }
            }

        } else {
            // The key is already in use, so we must modify it.
            int number = 0;
            if (!alwaysAddLetter && !firstLetterA) {
                number = 1;
            }

            String moddedKey = key + LabelPatternUtil.getAddition(number);
            occurrences = LabelPatternUtil.database.getNumberOfKeyOccurrences(moddedKey);

            if ((oldKey != null) && oldKey.equals(moddedKey)) {
                occurrences--;
            }

            while (occurrences > 0) {
                number++;
                moddedKey = key + LabelPatternUtil.getAddition(number);

                occurrences = LabelPatternUtil.database.getNumberOfKeyOccurrences(moddedKey);
                if ((oldKey != null) && oldKey.equals(moddedKey)) {
                    occurrences--;
                }
            }

            if (!moddedKey.equals(oldKey)) {
                if (LabelPatternUtil.database.getEntryById(entry.getId()) == null) {
                    // entry does not (yet) exist in the database, just update the entry
                    entry.setField(BibtexEntry.KEY_FIELD, moddedKey);
                } else {
                    LabelPatternUtil.database.setCiteKeyForEntry(entry.getId(), moddedKey);
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
        if (parts.length > offset) {
            for (int j = offset; j < parts.length; j++) {
                String modifier = parts[j];

                if (modifier.equals("lower")) {
                    label = label.toLowerCase();
                } else if (modifier.equals("upper")) {
                    label = label.toUpperCase();
                } else if (modifier.equals("abbr")) {
                    // Abbreviate - that is,
                    StringBuilder abbr = new StringBuilder();
                    String[] words = label.replaceAll("[\\{\\}']", "")
                            .split("[\\(\\) \r\n\"]");
                    for (String word1 : words) {
                        if (!word1.isEmpty()) {
                            abbr.append(word1.charAt(0));
                        }
                    }
                    label = abbr.toString();

                } else if (modifier.startsWith("(") && modifier.endsWith(")")) {
                    // Alternate text modifier in parentheses. Should be inserted if
                    // the label is empty:
                    if (label.equals("") && (modifier.length() > 2)) {
                        return modifier.substring(1, modifier.length() - 1);
                    }

                } else {
                    LOGGER.info("Key generator warning: unknown modifier '"
                            + modifier + "'.");
                }
            }
        }
        return label;
    }

    public static String makeLabel(BibtexEntry entry, String val) {

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
                    authString = LabelPatternUtil.normalize(LabelPatternUtil.database.resolveForStrings(authString));
                }

                if (val.startsWith("pure")) {
                    // remove the "pure" prefix so the remaining
                    // code in this section functions correctly
                    val = val.substring(4);
                } else {
                    if ((authString == null) || authString.equals("")) {
                        authString = entry.getField("editor");
                        if (authString != null) {
                            authString = LabelPatternUtil.normalize(
                                    LabelPatternUtil.database.resolveForStrings(authString));
                        }
                    }
                }

                // Gather all author-related checks, so we don't
                // have to check all the time.
                if (val.equals("auth")) {
                    return LabelPatternUtil.firstAuthor(authString);
                } else if (val.equals("authForeIni")) {
                    return LabelPatternUtil.firstAuthorForenameInitials(authString);
                } else if (val.equals("authFirstFull")) {
                    return LabelPatternUtil.firstAuthorVonAndLast(authString);
                } else if (val.equals("authors")) {
                    return LabelPatternUtil.allAuthors(authString);
                } else if (val.equals("authorsAlpha")) {
                    return LabelPatternUtil.authorsAlpha(authString);
                }
                // Last author's last name
                else if (val.equals("authorLast")) {
                    return LabelPatternUtil.lastAuthor(authString);
                } else if (val.equals("authorLastForeIni")) {
                    return LabelPatternUtil.lastAuthorForenameInitials(authString);
                } else if (val.equals("authorIni")) {
                    return LabelPatternUtil.oneAuthorPlusIni(authString);
                } else if (val.matches("authIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    String s = LabelPatternUtil.authIniN(authString, num);
                    return s == null ? "" : s;
                } else if (val.equals("auth.auth.ea")) {
                    String s = LabelPatternUtil.authAuthEa(authString);
                    return s == null ? "" : s;
                } else if (val.equals("auth.etal")) {
                    String s = LabelPatternUtil.authEtal(authString, ".", ".etal");
                    return s == null ? "" : s;
                } else if (val.equals("authEtAl")) {
                    String s = LabelPatternUtil.authEtal(authString, "", "EtAl");
                    return s == null ? "" : s;
                } else if (val.equals("authshort")) {
                    String s = LabelPatternUtil.authshort(authString);
                    return s == null ? "" : s;
                } else if (val.matches("auth[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    String s = LabelPatternUtil.authN_M(authString, Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]));
                    return s == null ? "" : s;
                } else if (val.matches("auth\\d+")) {
                    // authN. First N chars of the first author's last
                    // name.

                    int num = Integer.parseInt(val.substring(4));
                    String fa = LabelPatternUtil.firstAuthor(authString);
                    if (fa == null) {
                        return "";
                    }
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else if (val.matches("authors\\d+")) {
                    String s = LabelPatternUtil.NAuthors(authString, Integer.parseInt(val.substring(7)));
                    return s == null ? "" : s;
                } else {
                    // This "auth" business was a dead end, so just
                    // use it literally:
                    return LabelPatternUtil.getField(entry, val);
                }
            } else if (val.startsWith("ed")) {
                // Gather all markers starting with "ed" here, so we
                // don't have to check all the time.
                if (val.equals("edtr")) {
                    return LabelPatternUtil.firstAuthor(entry.getField("editor"));
                } else if (val.equals("edtrForeIni")) {
                    return LabelPatternUtil.firstAuthorForenameInitials(entry.getField("editor"));
                } else if (val.equals("editors")) {
                    return LabelPatternUtil.allAuthors(entry.getField("editor"));
                    // Last author's last name
                } else if (val.equals("editorLast")) {
                    return LabelPatternUtil.lastAuthor(entry.getField("editor"));
                } else if (val.equals("editorLastForeIni")) {
                    return LabelPatternUtil.lastAuthorForenameInitials(entry.getField("editor"));
                } else if (val.equals("editorIni")) {
                    return LabelPatternUtil.oneAuthorPlusIni(entry.getField("editor"));
                } else if (val.matches("edtrIni[\\d]+")) {
                    int num = Integer.parseInt(val.substring(7));
                    String s = LabelPatternUtil.authIniN(entry.getField("editor"), num);
                    return s == null ? "" : s;
                } else if (val.matches("edtr[\\d]+_[\\d]+")) {
                    String[] nums = val.substring(4).split("_");
                    String s = LabelPatternUtil.authN_M(entry.getField("editor"),
                            Integer.parseInt(nums[0]),
                            Integer.parseInt(nums[1]) - 1);
                    return s == null ? "" : s;
                } else if (val.equals("edtr.edtr.ea")) {
                    String s = LabelPatternUtil.authAuthEa(entry.getField("editor"));
                    return s == null ? "" : s;
                } else if (val.equals("edtrshort")) {
                    String s = LabelPatternUtil.authshort(entry.getField("editor"));
                    return s == null ? "" : s;
                }
                // authN. First N chars of the first author's last
                // name.
                else if (val.matches("edtr\\d+")) {
                    int num = Integer.parseInt(val.substring(4));
                    String fa = LabelPatternUtil.firstAuthor(entry.getField("editor"));
                    if (fa == null) {
                        return "";
                    }
                    if (num > fa.length()) {
                        num = fa.length();
                    }
                    return fa.substring(0, num);
                } else {
                    // This "ed" business was a dead end, so just
                    // use it literally:
                    return LabelPatternUtil.getField(entry, val);
                }
            } else if (val.equals("firstpage")) {
                return LabelPatternUtil.firstPage(entry.getField("pages"));
            } else if (val.equals("lastpage")) {
                return LabelPatternUtil.lastPage(entry.getField("pages"));
            } else if (val.equals("shorttitle")) {
                return LabelPatternUtil.getTitleWords(3, entry.getField("title"));
            } else if (val.equals("veryshorttitle")) {
                return LabelPatternUtil.getTitleWords(1, entry.getField("title"));
            } else if (val.equals("shortyear")) {
                String ss = entry.getFieldOrAlias("year");
                if (ss.startsWith("in") || ss.startsWith("sub")) {
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
                return LabelPatternUtil.getField(entry, val);
            }
        } catch (NullPointerException ex) {
            return "";
        }

    }

    /**
     * Look up a field of a BibtexEntry, returning its String value, or an
     * empty string if it isn't set.
     * @param entry The entry.
     * @param field The field to look up.
     * @return The field value.
     */
    private static String getField(BibtexEntry entry, String field) {
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
        if (number >= LabelPatternUtil.CHARS.length()) {
            int lastChar = number % LabelPatternUtil.CHARS.length();
            return LabelPatternUtil.getAddition((number / LabelPatternUtil.CHARS.length()) - 1) + LabelPatternUtil.CHARS.substring(lastChar, lastChar + 1);
        } else {
            return LabelPatternUtil.CHARS.substring(number, number + 1);
        }
    }

    /**
     * Determines "number" words out of the "title" field in the given BibTeX entry
     */
    static String getTitleWords(int number, String title) {
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
                stringBuilder.append(" ");
            }
            stringBuilder.append(word);
            words++;
        }

        return LabelPatternUtil.keepLettersAndDigitsOnly(stringBuilder.toString());
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
    static String firstAuthor(String authorField) {
        AuthorList authorList = AuthorList.getAuthorList(authorField);
        if (authorList.size() == 0) {
            return "";
        }
        String s = authorList.getAuthor(0).getLast();
        return s != null ? s : "";

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
    static String firstAuthorForenameInitials(String authorField) {
        AuthorList authorList = AuthorList.getAuthorList(authorField);
        if (authorList.size() == 0) {
            return "";
        }
        String s = authorList.getAuthor(0).getFirstAbbr();
        return s != null ? s.substring(0, 1) : "";
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
    static String firstAuthorVonAndLast(String authorField) {
        AuthorList authorList = AuthorList.getAuthorList(authorField);
        if (authorList.size() == 0) {
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
    static String lastAuthor(String authorField) {
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
     * Gets the forename initals of the last author/editor
     *
     * @param authorField
     *            a <code>String</code>
     * @return the forename initial of an author/editor or "" if no author was found
     *    This method is guaranteed to never return null.
     *
     * @throws NullPointerException
     *             if authorField == null
     */
    static String lastAuthorForenameInitials(String authorField) {
        AuthorList authorList = AuthorList.getAuthorList(authorField);
        if (authorList.size() == 0) {
            return "";
        }
        String s = authorList.getAuthor(authorList.size() - 1).getFirstAbbr();
        return s != null ? s.substring(0, 1) : "";
    }

    /**
     * Gets the last name of all authors/editors
     * @param authorField a <code>String</code>
     * @return the sur name of all authors/editors
     */
    static String allAuthors(String authorField) {
        // Quick hack to use NAuthors to avoid code duplication
        return NAuthors(authorField, Integer.MAX_VALUE);
    }

    /**
     * Returns the authors according to the BibTeX-alpha-Style
     * @param authorField string containing the value of the author field
     * @return the initials of all authornames
     */
    static String authorsAlpha(String authorField) {
        String authors = "";

        String fixedAuthors = AuthorList.fixAuthor_lastNameOnlyCommas(authorField, false);

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
    static String NAuthors(String authorField, int n) {
        String author = "";
        String[] tokens = AuthorList.fixAuthorForAlphabetization(authorField).split("\\s+\\band\\b\\s+");
        int i = 0;
        while ((tokens.length > i) && (i < n)) {
            String lastName = tokens[i].replaceAll(",\\s+.*", "");
            author += lastName;
            i++;
        }
        if (tokens.length <= n) {
            return author;
        }
        return author + "EtAl";
    }

    /**
     * Gets the first part of the last name of the first
     * author/editor, and appends the last name initial of the
     * remaining authors/editors.
     * Maximum 5 characters
     * @param authorField a <code>String</code>
     * @return the surname of all authors/editors
     */
    static String oneAuthorPlusIni(String authorField) {
        final int CHARS_OF_FIRST = 5;
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        String author = "";
        String[] tokens = authorField.split("\\s+\\band\\b\\s+");
        int i = 1;
        if (tokens.length == 0) {
            return author;
        }
        String firstAuthor = tokens[0].split(",")[0];
        author = firstAuthor.substring(0,
                Math.min(CHARS_OF_FIRST,
                        firstAuthor.length()));
        while (tokens.length > i) {
            // convert lastname, firstname to firstname lastname
            author += tokens[i].charAt(0);
            i++;
        }
        return author;
    }

    /**
     * auth.auth.ea format:
     * Isaac Newton and James Maxwell and Albert Einstein (1960)
     * Isaac Newton and James Maxwell (1960)
     *  give:
     * Newton.Maxwell.ea
     * Newton.Maxwell
     */
    static String authAuthEa(String authorField) {
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();

        String[] tokens = authorField.split("\\s+\\band\\b\\s+");
        if (tokens.length == 0) {
            return "";
        }
        // append first author
        author.append((tokens[0].split(","))[0]);
        if (tokens.length >= 2) {
            // append second author
            author.append(".").append((tokens[1].split(","))[0]);
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
    static String authEtal(String authorField, String delim,
            String append) {
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();

        String[] tokens = authorField.split("\\s*\\band\\b\\s*");
        if (tokens.length == 0) {
            return "";
        }
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
    static String authN_M(String authorField, int n, int m) {
        // have m counting from 0
        m--;

        authorField = AuthorList.fixAuthorForAlphabetization(authorField);

        String[] tokens = authorField.split("\\s+\\band\\b\\s+");
        if ((tokens.length <= m) || (n < 0) || (m < 0)) {
            return "";
        }
        String lastName = (tokens[m].split(","))[0];
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
    static String authshort(String authorField) {
        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();
        String[] tokens = authorField.split("\\band\\b");
        int i = 0;

        if (tokens.length == 1) {
            author.append(LabelPatternUtil.authN_M(authorField, authorField.length(), 1));
        } else if (tokens.length >= 2) {
            while ((tokens.length > i) && (i < 3)) {
                author.append(LabelPatternUtil.authN_M(authorField, 1, i+1));
                i++;
            }
            if (tokens.length > 3) {
                author.append("+");
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
    static String authIniN(String authorField, int n) {

        if (n <= 0) {
            return "";
        }

        authorField = AuthorList.fixAuthorForAlphabetization(authorField);
        StringBuilder author = new StringBuilder();
        String[] tokens = authorField.split("\\band\\b");
        int i = 0;
        int charsAll = n / tokens.length;

        if (tokens.length == 0) {
            return author.toString();
        }

        while (tokens.length > i) {
            if (i < (n % tokens.length)) {
                author.append(LabelPatternUtil.authN_M(authorField, charsAll + 1, i+1));
            } else {
                author.append(LabelPatternUtil.authN_M(authorField, charsAll, i+1));
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
        String[] _pages = pages.split("\\D+");
        int result = Integer.MAX_VALUE;
        for (String n : _pages) {
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
        String[] _pages = pages.split("\\D+");
        int result = Integer.MIN_VALUE;
        for (String n : _pages) {
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

}
