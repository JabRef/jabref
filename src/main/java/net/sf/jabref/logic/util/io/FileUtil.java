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
package net.sf.jabref.logic.util.io;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class FileUtil {
    private static final Log LOGGER = LogFactory.getLog(FileUtil.class);

    private static final String FILE_SEPARATOR = System.getProperty("file.separator");


    /**
     * Returns the extension of a file or Optional.empty() if the file does not have one (no . in name).
     *
     * @param file
     * @return The extension, trimmed and in lowercase.
     */
    public static Optional<String> getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    /**
     * Returns the extension of a file name or Optional.empty() if the file does not have one (no . in name).
     *
     * @param fileName
     * @return The extension, trimmed and in lowercase.
     */
    public static Optional<String> getFileExtension(String fileName) {
        int pos = fileName.lastIndexOf('.');
        if ((pos > 0) && (pos < (fileName.length() - 1))) {
            return Optional.of(fileName.substring(pos + 1).trim().toLowerCase());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Creates the minimal unique path substring for each file among multiple file paths.
     *
     * @param paths the file paths
     * @return the minimal unique path substring for each file path
     */
    public static List<String> uniquePathSubstrings(List<String> paths) {
        List<Stack<String>> stackList = new ArrayList<>(paths.size());
        // prepare data structures
        for (String path : paths) {
            List<String> directories = Arrays.asList(path.split(Pattern.quote(File.separator)));
            Stack<String> stack = new Stack<>();
            stack.addAll(directories);
            stackList.add(stack);
        }

        String[] arr = new String[paths.size()];
        Arrays.fill(arr, "");
        List<String> pathSubstrings = Arrays.asList(arr);

        // compute shortest folder substrings
        while(!stackList.stream().allMatch(Vector::isEmpty)) {
            for(int i = 0; i < stackList.size(); i++) {
                String tempString = pathSubstrings.get(i);

                if(tempString.isEmpty() && !stackList.get(i).isEmpty()) {
                    pathSubstrings.set(i, stackList.get(i).pop());
                } else {
                    if(!stackList.get(i).isEmpty()) {
                        pathSubstrings.set(i, stackList.get(i).pop() + File.separator + tempString);
                    }
                }
            }

            for(int i = 0; i < stackList.size(); i++) {
                String tempString = pathSubstrings.get(i);

                if(Collections.frequency(pathSubstrings, tempString) == 1) {
                    stackList.get(i).clear();
                }
            }
        }
        return pathSubstrings;
    }

    /**
     * Copies a file.
     *
     * @param source         File Source file
     * @param dest           File Destination file
     * @param deleteIfExists boolean Determines whether the copy goes on even if the file
     *                       exists.
     * @return boolean Whether the copy succeeded, or was stopped due to the
     * file already existing.
     * @throws IOException
     */
    public static boolean copyFile(File source, File dest, boolean deleteIfExists) throws IOException {

        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            // Check if the file already exists.
            if (dest.exists()) {
                if (!deleteIfExists) {
                    return false;
                    // else dest.delete();
                }
            }

            in = new BufferedInputStream(new FileInputStream(source));
            out = new BufferedOutputStream(new FileOutputStream(dest));
            int el;
            // int tell = 0;
            while ((el = in.read()) >= 0) {
                out.write(el);
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) {
                in.close();
            }
        }
        return true;
    }

    /**
     * @param fileName
     * @param destFilename
     * @return
     */
    public static boolean renameFile(String fileName, String destFilename) {
        // File (or directory) with old name
        File fromFile = new File(fileName);

        // File (or directory) with new name
        File toFile = new File(destFilename);

        // Rename file (or directory)
        return fromFile.renameTo(toFile);
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.<br/>
     * <p>
     * Uses <ul>
     * <li>the default directory associated with the extension of the file</li>
     * <li>the standard file directory</li>
     * <li>the directory of the bib file</li>
     * </ul>
     *
     * @param metaData The MetaData for the database this file belongs to.
     * @param name     The filename, may also be a relative path to the file
     */
    public static File expandFilename(final MetaData metaData, String name) {
        Optional<String> extension = getFileExtension(name);
        // Find the default directory for this field type, if any:
        String[] dir = metaData.getFileDirectory(extension.orElse(null));
        // Include the standard "file" directory:
        String[] fileDir = metaData.getFileDirectory(Globals.FILE_FIELD);
        // Include the directory of the bib file:
        ArrayList<String> al = new ArrayList<>();
        for (String aDir : dir) {
            if (!al.contains(aDir)) {
                al.add(aDir);
            }
        }
        for (String aFileDir : fileDir) {
            if (!al.contains(aFileDir)) {
                al.add(aFileDir);
            }
        }
        String[] dirs = al.toArray(new String[al.size()]);
        return expandFilename(name, dirs);
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.
     * <p>
     * Will look in each of the given dirs starting from the beginning and
     * returning the first found file to match if any.
     */
    public static File expandFilename(String name, String[] dir) {

        for (String aDir : dir) {
            if (aDir != null) {
                File result = expandFilename(name, aDir);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.
     */
    public static File expandFilename(String name, String dir) {

        File file;
        if ((name == null) || name.isEmpty()) {
            return null;
        } else {
            file = new File(name);
        }

        if (!file.exists() && (dir != null)) {
            if (dir.endsWith(FILE_SEPARATOR)) {
                name = dir + name;
            } else {
                name = dir + FILE_SEPARATOR + name;
            }

            // System.out.println("expanded to: "+name);
            // if (name.startsWith("ftp"))

            file = new File(name);

            if (file.exists()) {
                return file;
            }
            // Ok, try to fix / and \ problems:
            if (OS.WINDOWS) {
                // workaround for catching Java bug in regexp replacer
                // and, why, why, why ... I don't get it - wegner 2006/01/22
                try {
                    name = name.replaceAll("/", "\\\\");
                } catch (StringIndexOutOfBoundsException exc) {
                    LOGGER.error("An internal Java error was caused by the entry " + "\"" + name + "\"", exc);
                }
            } else {
                name = name.replaceAll("\\\\", "/");
            }
            // System.out.println("expandFilename: "+name);
            file = new File(name);
            if (!file.exists()) {
                file = null;
            }
        }
        return file;
    }

    /**
     * Converts an absolute filename to a relative one, if necessary.
     * Returns the parameter fileName itself if no shortening is possible
     * <p>
     * This method works correctly only if dirs are sorted decent in their length
     * i.e. /home/user/literature/important before /home/user/literature
     *
     * @param fileName the filename to be shortened
     * @param dirs     directories to check.
     */
    public static File shortenFileName(File fileName, String[] dirs) {
        if ((fileName == null) || (fileName.length() == 0)) {
            return fileName;
        }
        if (!fileName.isAbsolute() || (dirs == null)) {
            return fileName;
        }

        for (String dir : dirs) {
            if (dir != null) {
                File result = shortenFileName(fileName, dir);
                if ((result != null) && !result.equals(fileName)) {
                    return result;
                }
            }
        }
        return fileName;
    }

    private static File shortenFileName(File fileName, String dir) {
        if ((fileName == null) || (fileName.length() == 0)) {
            return fileName;
        }
        if (!fileName.isAbsolute() || (dir == null)) {
            return fileName;
        }

        String longName;
        if (OS.WINDOWS) {
            // case-insensitive matching on Windows
            longName = fileName.toString().toLowerCase();
            dir = dir.toLowerCase();
        } else {
            longName = fileName.toString();
        }

        if (!dir.endsWith(FILE_SEPARATOR)) {
            dir = dir.concat(FILE_SEPARATOR);
        }

        if (longName.startsWith(dir)) {
            // result is based on original name, not on lower-cased name
            String newName = fileName.toString().substring(dir.length());
            return new File(newName);
        } else {
            return fileName;
        }
    }

    public static Map<BibEntry, List<File>> findAssociatedFiles(Collection<BibEntry> entries, Collection<String> extensions, Collection<File> directories) {
        HashMap<BibEntry, List<File>> result = new HashMap<>();

        // First scan directories
        Set<File> filesWithExtension = FileFinder.findFiles(extensions, directories);

        // Initialize Result-Set
        for (BibEntry entry : entries) {
            result.put(entry, new ArrayList<>());
        }

        boolean exactOnly = Globals.prefs.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY);
        // Now look for keys
        nextFile: for (File file : filesWithExtension) {

            String name = file.getName();
            int dot = name.lastIndexOf('.');
            // First, look for exact matches:
            for (BibEntry entry : entries) {
                String citeKey = entry.getCiteKey();
                if ((citeKey != null) && !citeKey.isEmpty()) {
                    if (dot > 0) {
                        if (name.substring(0, dot).equals(citeKey)) {
                            result.get(entry).add(file);
                            continue nextFile;
                        }
                    }
                }
            }
            // If we get here, we didn't find any exact matches. If non-exact
            // matches are allowed, try to find one:
            if (!exactOnly) {
                for (BibEntry entry : entries) {
                    String citeKey = entry.getCiteKey();
                    if ((citeKey != null) && !citeKey.isEmpty()) {
                        if (name.startsWith(citeKey)) {
                            result.get(entry).add(file);
                            continue nextFile;
                        }
                    }
                }
            }
        }

        return result;
    }

    public static List<SimpleFileListEntry> decodeFileField(String value) {
        if (value == null) {
            value = "";
        }
        List<SimpleFileListEntry> newList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        List<String> thisEntry = new ArrayList<>();
        boolean inXmlChar = false;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && (c == '\\')) {
                escaped = true;
                continue;
            }
            // Check if we are entering an XML special character construct such
            // as "&#44;", because we need to know in order to ignore the semicolon.
            else if (!escaped && (c == '&') && !inXmlChar) {
                sb.append(c);
                if ((value.length() > (i + 1)) && (value.charAt(i + 1) == '#')) {
                    inXmlChar = true;
                }
            }
            // Check if we are exiting an XML special character construct:
            else if (!escaped && inXmlChar && (c == ';')) {
                sb.append(c);
                inXmlChar = false;
            } else if (!escaped && (c == ':')) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
            } else if (!escaped && (c == ';') && !inXmlChar) {
                thisEntry.add(sb.toString());
                newList.add(new SimpleFileListEntry(thisEntry));
                sb = new StringBuilder();
                thisEntry = new ArrayList<>();
            } else {
                sb.append(c);
            }
            escaped = false;
        }
        if (sb.length() > 0) {
            thisEntry.add(sb.toString());
        }
        if (!thisEntry.isEmpty()) {
            newList.add(new SimpleFileListEntry(thisEntry));
        }

        return newList;
    }

    /**
     * Returns the list of linked files. The files have the absolute filename
     *
     * @param bes list of BibTeX entries
     * @param fileDirs list of directories to try for expansion
     *
     * @return list of files. May be empty
     */
    public static List<File> getListOfLinkedFiles(BibEntry[] bes, String[] fileDirs) {
        ArrayList<File> result = new ArrayList<>();
        for (BibEntry entry : bes) {
            List<SimpleFileListEntry> fileList = decodeFileField(entry.getField(Globals.FILE_FIELD));
            for (SimpleFileListEntry file : fileList) {
                File f = expandFilename(file.getLink(), fileDirs);
                if (f != null) {
                    result.add(f);
                }
            }
        }

        return result;
    }

}
