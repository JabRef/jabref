package org.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RegExpFileSearch {

    private static final Log LOGGER = LogFactory.getLog(RegExpFileSearch.class);

    private static final String EXT_MARKER = "__EXTENSION__";

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("([^\\\\])\\\\([^\\\\])");

    private static final Pattern SQUARE_BRACKETS_PATTERN = Pattern.compile("\\[.*?\\]");


    private RegExpFileSearch() {
    }

    /**
     * Search for file links for a set of entries using regexp. Lists of extensions and directories
     * are given.
     * @param entries The entries to search for.
     * @param extensions The extensions that are acceptable.
     * @param directories The root directories to search.
     * @param regExp The expression deciding which names are acceptable.
     * @param keywordDelimiter
     * @return A map linking each given entry to a list of files matching the given criteria.
     */
    public static Map<BibEntry, List<File>> findFilesForSet(List<BibEntry> entries, List<String> extensions,
            List<File> directories, String regExp, Character keywordDelimiter) {

        Map<BibEntry, List<File>> res = new HashMap<>();
        for (BibEntry entry : entries) {
            res.put(entry, findFiles(entry, extensions, directories, regExp, keywordDelimiter));
        }
        return res;
    }

    /**
     * Method for searching for files using regexp. A list of extensions and directories can be
     * given.
     * @param entry The entry to search for.
     * @param extensions The extensions that are acceptable.
     * @param directories The root directories to search.
     * @param regularExpression The expression deciding which names are acceptable.
     * @param keywordDelimiter
     * @return A list of files paths matching the given criteria.
     */
    private static List<File> findFiles(BibEntry entry, List<String> extensions, List<File> directories,
            String regularExpression, Character keywordDelimiter) {

        String extensionRegExp = '(' + String.join("|", extensions) + ')';

        return findFile(entry, directories, regularExpression, extensionRegExp, keywordDelimiter);
    }

    /**
     * Searches the given directory and filename pattern for a file for the
     * BibTeX entry.
     *
     * Used to fix:
     *
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1503410&group_id=92314&atid=600309
     *
     * Requirements:
     *  - Be able to find the associated PDF in a set of given directories.
     *  - Be able to return a relative path or absolute path.
     *  - Be fast.
     *  - Allow for flexible naming schemes in the PDFs.
     *
     * Syntax scheme for file:
     * <ul>
     * <li>* Any subDir</li>
     * <li>** Any subDir (recursive)</li>
     * <li>[key] Key from BibTeX file and database</li>
     * <li>.* Anything else is taken to be a Regular expression.</li>
     * </ul>
     *
     * @param keywordDelimiter
     * @param entry
     *            non-null
     * @param dirs
     *            A set of root directories to start the search from. Paths are
     *            returned relative to these directories if relative is set to
     *            true. These directories will not be expanded or anything. Use
     *            the file attribute for this.
     * @param file
     *            non-null
     *
     * @return Will return the first file found to match the given criteria or
     *         null if none was found.
     */
    private static List<File> findFile(BibEntry entry, List<File> dirs, String file, String extensionRegExp,
            Character keywordDelimiter) {
        List<File> res = new ArrayList<>();
        for (File directory : dirs) {
            res.addAll(findFile(entry, directory.getPath(), file, extensionRegExp, keywordDelimiter));
        }
        return res;
    }

    /**
     * Internal Version of findFile, which also accepts a current directory to
     * base the search on.
     *
     */
    private static List<File> findFile(BibEntry entry, String directory, String file, String extensionRegExp,
            Character keywordDelimiter) {

        File root;
        if (directory == null) {
            root = new File(".");
        } else {
            root = new File(directory);
        }
        if (!root.exists()) {
            return Collections.emptyList();
        }
        List<File> fileList = RegExpFileSearch.findFile(entry, root, file, extensionRegExp, keywordDelimiter);

        List<File> result = new ArrayList<>();
        for (File tmpFile : fileList) {
            try {
                /**
                 * [ 1601651 ] PDF subdirectory - missing first character
                 *
                 * http://sourceforge.net/tracker/index.php?func=detail&aid=1601651&group_id=92314&atid=600306
                 */
                // Changed by M. Alver 2007.01.04:
                // Remove first character if it is a directory separator character:
                String tmp = tmpFile.getCanonicalPath().substring(root.getCanonicalPath().length());
                if ((tmp.length() > 1) && (tmp.charAt(0) == File.separatorChar)) {
                    tmp = tmp.substring(1);
                }
                result.add(new File(tmp));

            } catch (IOException e) {
                LOGGER.warn("Problem searching", e);
            }
        }
        return result;
    }

    /**
     * The actual work-horse. Will find absolute filepaths starting from the
     * given directory using the given regular expression string for search.
     */
    private static List<File> findFile(BibEntry entry, File directory, String file, String extensionRegExp,
            Character keywordDelimiter) {

        List<File> res = new ArrayList<>();

        String fileName = file;
        File actualDirectory;
        if (fileName.startsWith("/")) {
            actualDirectory = new File(".");
            fileName = fileName.substring(1);
        } else {
            actualDirectory = directory;
        }

        // Escape handling...
        Matcher m = ESCAPE_PATTERN.matcher(fileName);
        StringBuffer s = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(s, m.group(1) + '/' + m.group(2));
        }
        m.appendTail(s);
        fileName = s.toString();
        String[] fileParts = fileName.split("/");

        if (fileParts.length == 0) {
            return res;
        }

        for (int i = 0; i < (fileParts.length - 1); i++) {

            String dirToProcess = fileParts[i];
            dirToProcess = expandBrackets(dirToProcess, entry, null, keywordDelimiter);

            if (dirToProcess.matches("^.:$")) { // Windows Drive Letter
                actualDirectory = new File(dirToProcess + '/');
                continue;
            }
            if (".".equals(dirToProcess)) { // Stay in current directory
                continue;
            }
            if ("..".equals(dirToProcess)) {
                actualDirectory = new File(actualDirectory.getParent());
                continue;
            }
            if ("*".equals(dirToProcess)) { // Do for all direct subdirs

                File[] subDirs = actualDirectory.listFiles();
                if (subDirs != null) {
                    String restOfFileString = StringUtil.join(fileParts, "/", i + 1, fileParts.length);
                    for (File subDir : subDirs) {
                        if (subDir.isDirectory()) {
                            res.addAll(findFile(entry, subDir, restOfFileString, extensionRegExp, keywordDelimiter));
                        }
                    }
                }
            }
            // Do for all direct and indirect subdirs
            if ("**".equals(dirToProcess)) {
                List<File> toDo = new LinkedList<>();
                toDo.add(actualDirectory);

                String restOfFileString = StringUtil.join(fileParts, "/", i + 1, fileParts.length);

                while (!toDo.isEmpty()) {

                    // Get all subdirs of each of the elements found in toDo
                    File[] subDirs = toDo.remove(0).listFiles();
                    if (subDirs == null) {
                        continue;
                    }

                    toDo.addAll(Arrays.asList(subDirs));

                    for (File subDir : subDirs) {
                        if (!subDir.isDirectory()) {
                            continue;
                        }
                        res.addAll(findFile(entry, subDir, restOfFileString, extensionRegExp, keywordDelimiter));
                    }
                }

            } // End process directory information
        }

        // Last step: check if the given file can be found in this directory
        String filePart = fileParts[fileParts.length - 1].replace("[extension]", EXT_MARKER);
        String filenameToLookFor = expandBrackets(filePart, entry, null, keywordDelimiter).replaceAll(EXT_MARKER, extensionRegExp);
        final Pattern toMatch = Pattern.compile('^' + filenameToLookFor.replaceAll("\\\\\\\\", "\\\\") + '$',
                Pattern.CASE_INSENSITIVE);

        File[] matches = actualDirectory.listFiles((arg0, arg1) -> {
            return toMatch.matcher(arg1).matches();
        });
        if ((matches != null) && (matches.length > 0)) {
            Collections.addAll(res, matches);
        }
        return res;
    }

    /**
     * Takes a string that contains bracketed expression and expands each of these using getFieldAndFormat.
     * <p>
     * Unknown Bracket expressions are silently dropped.
     *
     * @param bracketString
     * @param entry
     * @param database
     * @param keywordDelimiter
     * @return
     */
    public static String expandBrackets(String bracketString, BibEntry entry, BibDatabase database,
            Character keywordDelimiter) {
        Matcher m = SQUARE_BRACKETS_PATTERN.matcher(bracketString);
        StringBuffer s = new StringBuffer();
        while (m.find()) {
            String replacement = getFieldAndFormat(m.group(), entry, database, keywordDelimiter);
            m.appendReplacement(s, replacement);
        }
        m.appendTail(s);

        return s.toString();
    }

    /**
     * Accepts a string like [author:lower] or [title:abbr] or [auth], whereas the first part signifies the bibtex-field
     * to get, or the key generator field marker to use, while the others are the modifiers that will be applied.
     *
     * @param fieldAndFormat
     * @param entry
     * @param database
     * @param keywordDelimiter
     * @return
     */
    public static String getFieldAndFormat(String fieldAndFormat, BibEntry entry, BibDatabase database,
            Character keywordDelimiter) {

        String strippedFieldAndFormat = StringUtil.stripBrackets(fieldAndFormat);

        int colon = strippedFieldAndFormat.indexOf(':');

        String beforeColon;
        String afterColon;
        if (colon == -1) {
            beforeColon = strippedFieldAndFormat;
            afterColon = null;
        } else {
            beforeColon = strippedFieldAndFormat.substring(0, colon);
            afterColon = strippedFieldAndFormat.substring(colon + 1);
        }
        beforeColon = beforeColon.trim();

        if (beforeColon.isEmpty()) {
            return "";
        }

        // If no field value was found, try to interpret it as a key generator field marker:
        String fieldValue = entry.getResolvedFieldOrAlias(beforeColon, database)
                .orElse(BibtexKeyPatternUtil.makeLabel(entry, beforeColon, keywordDelimiter, database));

        if (fieldValue == null) {
            return "";
        }

        if ((afterColon == null) || afterColon.isEmpty()) {
            return fieldValue;
        }

        List<String> parts = Arrays.asList(afterColon.split(":"));
        fieldValue = BibtexKeyPatternUtil.applyModifiers(fieldValue, parts, 0);

        return fieldValue;
    }

}
