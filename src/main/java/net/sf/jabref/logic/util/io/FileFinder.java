package net.sf.jabref.logic.util.io;

import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.util.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileFinder {

    /**
     * Searches the given directory and subdirectories for a pdf file with name
     * as given + ".pdf"
     */
    public static String findPdf(String key, String extension, String directory, FilenameFilter off) {
        // String filename = key + "."+extension;

        /*
         * Simon Fischer's patch for replacing a regexp in keys before
         * converting to filename:
         *
         * String regex = Globals.prefs.get("basenamePatternRegex"); if ((regex !=
         * null) && (regex.trim().length() > 0)) { String replacement =
         * Globals.prefs.get("basenamePatternReplacement"); key =
         * key.replaceAll(regex, replacement); }
         */
        if (!directory.endsWith(System.getProperty("file.separator"))) {
            directory += System.getProperty("file.separator");
        }
        String found = FileFinder.findInDir(key, directory, off, 0);
        if (found != null) {
            return found.substring(directory.length());
        } else {
            return null;
        }
    }

    public static Set<File> findFiles(Collection<String> extensions, Collection<File> directories) {
        Set<File> result = new HashSet<>();

        for (File directory : directories) {
            result.addAll(FileFinder.findFiles(extensions, directory));
        }

        return result;
    }

    private static Collection<? extends File> findFiles(Collection<String> extensions, File directory) {
        Set<File> result = new HashSet<>();

        File[] children = directory.listFiles();
        if (children == null)
         {
            return result; // No permission?
        }

        for (File child : children) {
            if (child.isDirectory()) {
                result.addAll(FileFinder.findFiles(extensions, child));
            } else {

                String extension = FileUtil.getFileExtension(child);

                if (extension != null) {
                    if (extensions.contains(extension)) {
                        result.add(child);
                    }
                }
            }
        }

        return result;
    }

    /**
     * New version of findPdf that uses findFiles.
     *
     * The search pattern will be read from the preferences.
     *
     * The [extension]-tags in this pattern will be replace by the given
     * extension parameter.
     *
     */
    public static String findPdf(BibtexEntry entry, String extension, String directory) {
        return FileFinder.findPdf(entry, extension, new String[]{directory});
    }

    /**
     * Convenience method for findPDF. Can search multiple PDF directories.
     */
    public static String findPdf(BibtexEntry entry, String extension, String[] directories) {

        String regularExpression;
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) {
            regularExpression = Globals.prefs.get(JabRefPreferences.REG_EXP_SEARCH_EXPRESSION_KEY);
        } else {
            regularExpression = Globals.prefs
                    .get(JabRefPreferences.DEFAULT_REG_EXP_SEARCH_EXPRESSION_KEY);
        }
        regularExpression = regularExpression.replaceAll("\\[extension\\]", extension);

        return FileFinder.findFile(entry, null, directories, regularExpression, true);
    }

    /**
     * Convenience menthod for findPDF. Searches for a file of the given type.
     * @param entry The BibtexEntry to search for a link for.
     * @param fileType The file type to search for.
     * @return The link to the file found, or null if not found.
     */
    public static String findFile(BibtexEntry entry, ExternalFileType fileType, List<String> extraDirs) {

        List<String> dirs = new ArrayList<>();
        dirs.addAll(extraDirs);
        if (Globals.prefs.hasKey(fileType.getExtension() + "Directory")) {
            dirs.add(Globals.prefs.get(fileType.getExtension() + "Directory"));
        }
        String[] directories = dirs.toArray(new String[dirs.size()]);
        return FileFinder.findPdf(entry, fileType.getExtension(), directories);
    }

    /**
     * Searches the given directory and filename pattern for a file for the
     * bibtexentry.
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
     * <li>** Any subDir (recursiv)</li>
     * <li>[key] Key from bibtex file and database</li>
     * <li>.* Anything else is taken to be a Regular expression.</li>
     * </ul>
     *
     * @param entry
     *            non-null
     * @param database
     *            non-null
     * @param directory
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
    private static String findFile(BibtexEntry entry, BibtexDatabase database, String[] directory,
                                   String file, boolean relative) {

        for (String aDirectory : directory) {
            String result = FileFinder.findFile(entry, database, aDirectory, file, relative);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Convenience function for absolute search.
     *
     * Uses findFile(BibtexEntry, BibtexDatabase, (String)null, String, false).
     */
    public static String findFile(BibtexEntry entry, BibtexDatabase database, String file) {
        return FileFinder.findFile(entry, database, (String) null, file, false);
    }

    /**
     * Internal Version of findFile, which also accepts a current directory to
     * base the search on.
     *
     */
    public static String findFile(BibtexEntry entry, BibtexDatabase database, String directory,
            String file, boolean relative) {

        File root;
        if (directory == null) {
            root = new File(".");
        } else {
            root = new File(directory);
        }
        if (!root.exists()) {
            return null;
        }

        String found = FileFinder.findFile(entry, database, root, file);

        if (directory == null || !relative) {
            return found;
        }

        if (found != null) {
            try {
                /**
                 * [ 1601651 ] PDF subdirectory - missing first character
                 *
                 * http://sourceforge.net/tracker/index.php?func=detail&aid=1601651&group_id=92314&atid=600306
                 */
                // Changed by M. Alver 2007.01.04:
                // Remove first character if it is a directory separator character:
                String tmp = found.substring(root.getCanonicalPath().length());
                if (tmp.length() > 1 && tmp.charAt(0) == File.separatorChar) {
                    tmp = tmp.substring(1);
                }
                return tmp;
                //return found.substring(root.getCanonicalPath().length());
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * The actual work-horse. Will find absolute filepaths starting from the
     * given directory using the given regular expression string for search.
     */
    private static String findFile(BibtexEntry entry, BibtexDatabase database, File directory,
                                   String file) {

        if (file.startsWith("/")) {
            directory = new File(".");
            file = file.substring(1);
        }

        // Escape handling...
        Matcher m = Pattern.compile("([^\\\\])\\\\([^\\\\])").matcher(file);
        StringBuffer s = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(s, m.group(1) + '/' + m.group(2));
        }
        m.appendTail(s);
        file = s.toString();
        String[] fileParts = file.split("/");

        if (fileParts.length == 0) {
            return null;
        }

        if (fileParts.length > 1) {

            for (int i = 0; i < fileParts.length - 1; i++) {

                String dirToProcess = fileParts[i];

                dirToProcess = Util.expandBrackets(dirToProcess, entry, database);

                if (dirToProcess.matches("^.:$")) { // Windows Drive Letter
                    directory = new File(dirToProcess + '/');
                    continue;
                }
                if (dirToProcess.equals(".")) { // Stay in current directory
                    continue;
                }
                if (dirToProcess.equals("..")) {
                    directory = new File(directory.getParent());
                    continue;
                }
                if (dirToProcess.equals("*")) { // Do for all direct subdirs

                    File[] subDirs = directory.listFiles();
                    if (subDirs == null)
                     {
                        return null; // No permission?
                    }

                    String restOfFileString = StringUtil.join(fileParts, "/", i + 1, fileParts.length);

                    for (File subDir : subDirs) {
                        if (subDir.isDirectory()) {
                            String result = FileFinder.findFile(entry, database, subDir,
                                    restOfFileString);
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                    return null;
                }
                // Do for all direct and indirect subdirs
                if (dirToProcess.equals("**")) {
                    List<File> toDo = new LinkedList<>();
                    toDo.add(directory);

                    String restOfFileString = StringUtil.join(fileParts, "/", i + 1, fileParts.length);

                    // Before checking the subdirs, we first check the current
                    // dir
                    String result = FileFinder.findFile(entry, database, directory, restOfFileString);
                    if (result != null) {
                        return result;
                    }

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
                            result = FileFinder.findFile(entry, database, subDir, restOfFileString);
                            if (result != null) {
                                return result;
                            }
                        }
                    }
                    // We already did the currentDirectory
                    return null;
                }

                final Pattern toMatch = Pattern
                        .compile(dirToProcess.replaceAll("\\\\\\\\", "\\\\"));

                File[] matches = directory.listFiles((arg0, arg1) -> toMatch.matcher(arg1).matches());
                if (matches == null || matches.length == 0) {
                    return null;
                }

                directory = matches[0];

                if (!directory.exists()) {
                    return null;
                }

            } // End process directory information
        }
        // Last step check if the given file can be found in this directory
        String filenameToLookFor = Util.expandBrackets(fileParts[fileParts.length - 1], entry, database);

        final Pattern toMatch = Pattern.compile('^'
                + filenameToLookFor.replaceAll("\\\\\\\\", "\\\\") + '$');

        File[] matches = directory.listFiles((arg0, arg1) -> toMatch.matcher(arg1).matches());
        if (matches == null || matches.length == 0) {
            return null;
        }

        try {
            return matches[0].getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    private static String findInDir(String key, String dir, FilenameFilter off, int count) {
        if (count > 20) {
            return null; // Make sure an infinite loop doesn't occur.
        }
        File f = new File(dir);
        File[] all = f.listFiles();
        if (all == null) {
            return null; // An error occured. We may not have permission to list the files.
        }

        for (File curFile : all) {
            if (curFile.isFile()) {
                String name = curFile.getName();
                if (name.startsWith(key + '.') && off.accept(f, name)) {
                    return curFile.getPath();
                }

            } else if (curFile.isDirectory()) {
                String found = FileFinder.findInDir(key, curFile.getPath(), off, count + 1);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
