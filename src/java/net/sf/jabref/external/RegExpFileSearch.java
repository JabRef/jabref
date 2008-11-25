package net.sf.jabref.external;

import net.sf.jabref.*;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Apr 12, 2008
 * Time: 1:46:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class RegExpFileSearch {

    final static String EXT_MARKER = "__EXTENSION__";

    public static void main(String[] args) {
        BibtexEntry entry = new BibtexEntry(Util.createNeutralId());
        entry.setField(BibtexFields.KEY_FIELD, "raffel01");
        entry.setField("year", "2001");
        ArrayList<String> extensions = new ArrayList<String>();
        extensions.add("pdf");
        extensions.add("ps");
        extensions.add("txt");
        List<File> dirs = new ArrayList<File>();
        dirs.add(new File("/home/alver/Desktop/Tromso_2008"));
        System.out.println(findFiles(entry, extensions, dirs,
                "**/[bibtexkey].*\\\\.[extension]"));
    }

    /**
     * Search for file links for a set of entries using regexp. Lists of extensions and directories
     * are given.
     * @param entries The entries to search for.
     * @param extensions The extensions that are acceptable.
     * @param directories The root directories to search.
     * @param regExp The expression deciding which names are acceptable.
     * @return A map linking each given entry to a list of files matching the given criteria.
     */
    public static Map<BibtexEntry, java.util.List<File>> findFilesForSet(Collection<BibtexEntry> entries,
                 Collection<String> extensions, List<File> directories, String regExp) {

        Map<BibtexEntry, java.util.List<File>> res = new HashMap<BibtexEntry, List<File>>();
        for (BibtexEntry entry : entries) {
            res.put(entry, findFiles(entry, extensions, directories, regExp));
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
    public static List<File> findFiles(BibtexEntry entry, Collection<String> extensions,
                                       Collection<File> directories, String regularExpression) {

        StringBuilder sb = new StringBuilder();
        for (Iterator<String> i = extensions.iterator(); i.hasNext();) {
            sb.append(i.next());
            if (i.hasNext())
                    sb.append("|");
        }
        String extensionRegExp = "("+sb.toString()+")";

        return findFile(entry, null, directories, regularExpression, extensionRegExp, true);
    }

        /**
	 * Searches the given directory and file name pattern for a file for the
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
	public static List<File> findFile(BibtexEntry entry, BibtexDatabase database, Collection<File> dirs,
		String file, String extensionRegExp, boolean relative) {
        ArrayList<File> res = new ArrayList<File>();
		for (File directory : dirs) {
            List<File> tmp = findFile(entry, database, directory.getPath(), file, extensionRegExp, relative);
            if (tmp != null)
                res.addAll(tmp);
		}
		return res;
	}

    /**
     * Internal Version of findFile, which also accepts a current directory to
     * base the search on.
     *
     */
    public static List<File> findFile(BibtexEntry entry, BibtexDatabase database, String directory,
        String file, String extensionRegExp, boolean relative) {

        List<File> res;
        File root;
        if (directory == null) {
            root = new File(".");
        } else {
            root = new File(directory);
        }
        if (!root.exists()) {
            return null;
        }
        res = findFile(entry, database, root, file, extensionRegExp);


        if (res.size() > 0) {
            for (int i=0; i<res.size(); i++)
                try {
                    /**
                     * [ 1601651 ] PDF subdirectory - missing first character
                     *
                     * http://sourceforge.net/tracker/index.php?func=detail&aid=1601651&group_id=92314&atid=600306
                     */
                    // Changed by M. Alver 2007.01.04:
                    // Remove first character if it is a directory separator character:
                    String tmp = res.get(i).getCanonicalPath().substring(root.getCanonicalPath().length());
                    if ((tmp.length() > 1) && (tmp.charAt(0) == File.separatorChar))
                        tmp = tmp.substring(1);
                    res.set(i, new File(tmp));
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return res;
    }

    /**
     * The actual work-horse. Will find absolute filepaths starting from the
     * given directory using the given regular expression string for search.
     */
    protected static List<File> findFile(BibtexEntry entry, BibtexDatabase database, File directory,
        String file, String extensionRegExp) {

        ArrayList<File> res = new ArrayList<File>();

        if (file.startsWith("/")) {
            directory = new File(".");
            file = file.substring(1);
        }

        // Escape handling...
        Matcher m = Pattern.compile("([^\\\\])\\\\([^\\\\])").matcher(file);
        StringBuffer s = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(s, m.group(1) + "/" + m.group(2));
        }
        m.appendTail(s);
        file = s.toString();
        String[] fileParts = file.split("/");

        if (fileParts.length == 0)
            return res;

        if (fileParts.length > 1) {

            for (int i = 0; i < fileParts.length - 1; i++) {

                String dirToProcess = fileParts[i];
                dirToProcess = Util.expandBrackets(dirToProcess, entry, database);

                if (dirToProcess.matches("^.:$")) { // Windows Drive Letter
                    directory = new File(dirToProcess + "/");
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
                    if (subDirs != null) {
                        String restOfFileString = Util.join(fileParts, "/", i + 1, fileParts.length);
                        for (int sub = 0; sub < subDirs.length; sub++) {
                            if (subDirs[sub].isDirectory()) {
                                res.addAll(findFile(entry, database, subDirs[sub],
                                    restOfFileString, extensionRegExp));
                            }
                        }
                    }
                }
                // Do for all direct and indirect subdirs
                if (dirToProcess.equals("**")) {
                    List<File> toDo = new LinkedList<File>();
                    toDo.add(directory);

                    String restOfFileString = Util.join(fileParts, "/", i + 1, fileParts.length);

                    while (!toDo.isEmpty()) {

                        // Get all subdirs of each of the elements found in toDo
                        File[] subDirs = toDo.remove(0).listFiles();
                        if (subDirs == null) // No permission?
                            continue;

                        toDo.addAll(Arrays.asList(subDirs));

                        for (int sub = 0; sub < subDirs.length; sub++) {
                            if (!subDirs[sub].isDirectory())
                                continue;
                            res.addAll(findFile(entry, database, subDirs[sub], restOfFileString,
                                    extensionRegExp));
                        }
                    }

                }

            } // End process directory information
        }

        // Last step: check if the given file can be found in this directory
        String filePart = fileParts[fileParts.length-1].replaceAll("\\[extension\\]", EXT_MARKER);
        String filenameToLookFor = Util.expandBrackets(filePart, entry, database)
                .replaceAll(EXT_MARKER, extensionRegExp);
        final Pattern toMatch = Pattern.compile("^"
            + filenameToLookFor.replaceAll("\\\\\\\\", "\\\\") + "$", Pattern.CASE_INSENSITIVE);

        File[] matches = directory.listFiles(new FilenameFilter() {
            public boolean accept(File arg0, String arg1) {
                return toMatch.matcher(arg1).matches();
            }
        });
        if (matches != null && (matches.length > 0))
            for (int i = 0; i < matches.length; i++) {
                File match = matches[i];
                res.add(match);
            }
        return res;
    }




}
