/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.external;

import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Apr 12, 2008
 * Time: 1:46:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegExpFileSearch {

    private static final Log LOGGER = LogFactory.getLog(RegExpFileSearch.class);

    private static final String EXT_MARKER = "__EXTENSION__";

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("([^\\\\])\\\\([^\\\\])");

    /**
     * Search for file links for a set of entries using regexp. Lists of extensions and directories
     * are given.
     * @param entries The entries to search for.
     * @param extensions The extensions that are acceptable.
     * @param directories The root directories to search.
     * @param regExp The expression deciding which names are acceptable.
     * @return A map linking each given entry to a list of files matching the given criteria.
     */
    public static Map<BibEntry, List<File>> findFilesForSet(Collection<BibEntry> entries,
            Collection<String> extensions, List<File> directories, String regExp) {

        Map<BibEntry, List<File>> res = new HashMap<>();
        for (BibEntry entry : entries) {
            res.put(entry, RegExpFileSearch.findFiles(entry, extensions, directories, regExp));
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
     * @return A list of files paths matching the given criteria.
     */
    private static List<File> findFiles(BibEntry entry, Collection<String> extensions,
            Collection<File> directories, String regularExpression) {

        String extensionRegExp = '(' + String.join("|", extensions) + ')';

        return RegExpFileSearch.findFile(entry, directories, regularExpression, extensionRegExp);
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
     * @param file
     *            non-null
     *
     * @param relative
     *            whether to return relative file paths or absolute ones
     *
     * @return Will return the first file found to match the given criteria or
     *         null if none was found.
     */
    private static List<File> findFile(BibEntry entry, Collection<File> dirs, String file,
            String extensionRegExp) {
        List<File> res = new ArrayList<>();
        for (File directory : dirs) {
            res.addAll(RegExpFileSearch.findFile(entry, directory.getPath(), file, extensionRegExp));
        }
        return res;
    }

    /**
     * Internal Version of findFile, which also accepts a current directory to
     * base the search on.
     *
     */
    private static List<File> findFile(BibEntry entry, String directory, String file, String extensionRegExp) {

        File root;
        if (directory == null) {
            root = new File(".");
        } else {
            root = new File(directory);
        }
        if (!root.exists()) {
            return Collections.emptyList();
        }
        List<File> res = RegExpFileSearch.findFile(entry, root, file, extensionRegExp);

        if (!res.isEmpty()) {
            for (int i = 0; i < res.size(); i++) {
                try {
                    /**
                     * [ 1601651 ] PDF subdirectory - missing first character
                     *
                     * http://sourceforge.net/tracker/index.php?func=detail&aid=1601651&group_id=92314&atid=600306
                     */
                    // Changed by M. Alver 2007.01.04:
                    // Remove first character if it is a directory separator character:
                    String tmp = res.get(i).getCanonicalPath().substring(root.getCanonicalPath().length());
                    if ((tmp.length() > 1) && (tmp.charAt(0) == File.separatorChar)) {
                        tmp = tmp.substring(1);
                    }
                    res.set(i, new File(tmp));

                } catch (IOException e) {
                    LOGGER.warn("Problem searching", e);
                }
            }
        }
        return res;
    }

    /**
     * The actual work-horse. Will find absolute filepaths starting from the
     * given directory using the given regular expression string for search.
     */
    private static List<File> findFile(BibEntry entry, File directory, String file, String extensionRegExp) {

        List<File> res = new ArrayList<>();

        File actualDirectory;
        if (file.startsWith("/")) {
            actualDirectory = new File(".");
            file = file.substring(1);
        } else {
            actualDirectory = directory;
        }

        // Escape handling...
        Matcher m = ESCAPE_PATTERN.matcher(file);
        StringBuffer s = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(s, m.group(1) + '/' + m.group(2));
        }
        m.appendTail(s);
        file = s.toString();
        String[] fileParts = file.split("/");

        if (fileParts.length == 0) {
            return res;
        }

        if (fileParts.length > 1) {

            for (int i = 0; i < (fileParts.length - 1); i++) {

                String dirToProcess = fileParts[i];
                dirToProcess = Util.expandBrackets(dirToProcess, entry, null);

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
                                res.addAll(RegExpFileSearch.findFile(entry, subDir, restOfFileString, extensionRegExp));
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
                            res.addAll(RegExpFileSearch.findFile(entry, subDir, restOfFileString, extensionRegExp));
                        }
                    }

                }

            } // End process directory information
        }

        // Last step: check if the given file can be found in this directory
        String filePart = fileParts[fileParts.length - 1].replace("[extension]", RegExpFileSearch.EXT_MARKER);
        String filenameToLookFor = Util.expandBrackets(filePart, entry, null)
                .replaceAll(RegExpFileSearch.EXT_MARKER, extensionRegExp);
        final Pattern toMatch = Pattern.compile('^'
                + filenameToLookFor.replaceAll("\\\\\\\\", "\\\\") + '$', Pattern.CASE_INSENSITIVE);

        File[] matches = actualDirectory.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File arg0, String arg1) {
                return toMatch.matcher(arg1).matches();
            }
        });
        if ((matches != null) && (matches.length > 0)) {
            Collections.addAll(res, matches);
        }
        return res;
    }

}
