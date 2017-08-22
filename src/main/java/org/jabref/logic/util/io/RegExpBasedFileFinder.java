package org.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class RegExpBasedFileFinder implements FileFinder {
    private static final Log LOGGER = LogFactory.getLog(RegExpBasedFileFinder.class);

    private static final String EXT_MARKER = "__EXTENSION__";

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("([^\\\\])\\\\([^\\\\])");

    private static final Pattern SQUARE_BRACKETS_PATTERN = Pattern.compile("\\[.*?\\]");
    private String regExp;
    private Character keywordDelimiter;

    /**
     * @param regExp The expression deciding which names are acceptable.
     */
    RegExpBasedFileFinder(String regExp, Character keywordDelimiter) {
        this.regExp = regExp;
        this.keywordDelimiter = keywordDelimiter;
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

    @Override
    public Map<BibEntry, List<Path>> findAssociatedFiles(List<BibEntry> entries, List<Path> directories, List<String> extensions) {
        Map<BibEntry, List<Path>> res = new HashMap<>();
        for (BibEntry entry : entries) {
            res.put(entry, findFiles(entry, extensions, directories));
        }
        return res;
    }

    /**
     * Method for searching for files using regexp. A list of extensions and directories can be
     * given.
     * @param entry The entry to search for.
     * @param extensions The extensions that are acceptable.
     * @param directories The root directories to search.
     * @return A list of files paths matching the given criteria.
     */
    private List<Path> findFiles(BibEntry entry, List<String> extensions, List<Path> directories) {
        String extensionRegExp = '(' + String.join("|", extensions) + ')';
        return findFile(entry, directories, extensionRegExp);
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
     * @param entry
     *            non-null
     * @param dirs
     *            A set of root directories to start the search from. Paths are
     *            returned relative to these directories if relative is set to
     *            true. These directories will not be expanded or anything. Use
     *            the file attribute for this.
     *
     * @return Will return the first file found to match the given criteria or
     *         null if none was found.
     */
    private List<Path> findFile(BibEntry entry, List<Path> dirs, String extensionRegExp) {
        List<Path> res = new ArrayList<>();
        for (Path directory : dirs) {
            res.addAll(findFile(entry, directory, regExp, extensionRegExp));
        }
        return res;
    }

    /**
     * The actual work-horse. Will find absolute filepaths starting from the
     * given directory using the given regular expression string for search.
     */
    private List<Path> findFile(BibEntry entry, Path directory, String file, String extensionRegExp) {
        List<Path> res = new ArrayList<>();

        String fileName = file;
        Path actualDirectory;
        if (fileName.startsWith("/")) {
            actualDirectory = Paths.get(".");
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
                actualDirectory = Paths.get(dirToProcess + '/');
                continue;
            }
            if (".".equals(dirToProcess)) { // Stay in current directory
                continue;
            }
            if ("..".equals(dirToProcess)) {
                actualDirectory = actualDirectory.getParent();
                continue;
            }
            if ("*".equals(dirToProcess)) { // Do for all direct subdirs
                File[] subDirs = actualDirectory.toFile().listFiles();
                if (subDirs != null) {
                    String restOfFileString = StringUtil.join(fileParts, "/", i + 1, fileParts.length);
                    for (File subDir : subDirs) {
                        if (subDir.isDirectory()) {
                            res.addAll(findFile(entry, subDir.toPath(), restOfFileString, extensionRegExp));
                        }
                    }
                }
            }
            // Do for all direct and indirect subdirs
            if ("**".equals(dirToProcess)) {
                String restOfFileString = StringUtil.join(fileParts, "/", i + 1, fileParts.length);

                try {
                    Path finalActualDirectory = actualDirectory;
                    Files.walk(actualDirectory).forEach(subElement -> {
                        // We only want to transverse directory (and not the current one; this is already done below)
                        if (!finalActualDirectory.equals(subElement) && Files.isDirectory(subElement)) {
                            res.addAll(findFile(entry, subElement, restOfFileString, extensionRegExp));
                        }
                    });
                } catch (IOException e) {
                    LOGGER.debug(e);
                }
            } // End process directory information
        }

        // Last step: check if the given file can be found in this directory
        String filePart = fileParts[fileParts.length - 1].replace("[extension]", EXT_MARKER);
        String filenameToLookFor = expandBrackets(filePart, entry, null, keywordDelimiter).replaceAll(EXT_MARKER, extensionRegExp);
        final Pattern toMatch = Pattern.compile('^' + filenameToLookFor.replaceAll("\\\\\\\\", "\\\\") + '$',
                Pattern.CASE_INSENSITIVE);
        try {
            List<Path> matches = Files.find(actualDirectory, 1,
                    (path, attributes) -> toMatch.matcher(path.getFileName().toString()).matches())
                    .collect(Collectors.toList());
            res.addAll(matches);
        } catch (IOException e) {
            LOGGER.debug(e);
        }
        return res;
    }
}
