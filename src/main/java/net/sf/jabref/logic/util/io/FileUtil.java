package net.sf.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;

import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FileField;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.metadata.FileDirectoryPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileUtil {

    private static final Log LOGGER = LogFactory.getLog(FileUtil.class);

    private static final Pattern SLASH = Pattern.compile("/");
    private static final Pattern BACKSLASH = Pattern.compile("\\\\");

    public static final boolean isPosixCompilant = FileSystems.getDefault().supportedFileAttributeViews().contains(
            "posix");

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
     * Returns the extension of a file name or Optional.empty() if the file does not have one (no "." in name).
     *
     * @param fileName
     * @return The extension (without leading dot), trimmed and in lowercase.
     */
    public static Optional<String> getFileExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');
        if ((dotPosition > 0) && (dotPosition < (fileName.length() - 1))) {
            return Optional.of(fileName.substring(dotPosition + 1).trim().toLowerCase());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the name part of a file name (i.e., everything in front of last ".").
     */
    public static String getFileName(String fileNameWithExtension) {
        int dotPosition = fileNameWithExtension.lastIndexOf('.');
        if (dotPosition >= 0) {
            return fileNameWithExtension.substring(0, dotPosition);
        } else {
            return fileNameWithExtension;
        }
    }

    /**
     * Adds an extension to the given file name. The original extension is not replaced. That means,
     * "demo.bib", ".sav" gets "demo.bib.sav" and not "demo.sav"
     *
     * @param path the path to add the extension to
     * @param extension the extension to add
     * @return the with the modified file name
     */
    public static Path addExtension(Path path, String extension) {
        Path fileName = path.getFileName();
        return path.resolveSibling(fileName + extension);
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

        List<String> pathSubstrings = new ArrayList<>(Collections.nCopies(paths.size(), ""));

        // compute shortest folder substrings
        while (!stackList.stream().allMatch(Vector::isEmpty)) {
            for (int i = 0; i < stackList.size(); i++) {
                String tempString = pathSubstrings.get(i);

                if (tempString.isEmpty() && !stackList.get(i).isEmpty()) {
                    pathSubstrings.set(i, stackList.get(i).pop());
                } else if (!stackList.get(i).isEmpty()) {
                    pathSubstrings.set(i, stackList.get(i).pop() + File.separator + tempString);
                }
            }

            for (int i = 0; i < stackList.size(); i++) {
                String tempString = pathSubstrings.get(i);

                if (Collections.frequency(pathSubstrings, tempString) == 1) {
                    stackList.get(i).clear();
                }
            }
        }
        return pathSubstrings;
    }

    /**
     * Copies a file.
     *
     * @param pathToSourceFile      Path Source file
     * @param pathToDestinationFile Path Destination file
     * @param replaceExisting       boolean Determines whether the copy goes on even if the file exists.
     * @return boolean Whether the copy succeeded, or was stopped due to the file already existing.
     * @throws IOException
     */
    public static boolean copyFile(Path pathToSourceFile, Path pathToDestinationFile, boolean replaceExisting) {
        // Check if the file already exists.
        if (!Files.exists(pathToSourceFile)) {
            LOGGER.error("Path to the source file doesn't exist.");
            return false;
        }
        if (Files.exists(pathToDestinationFile) && !replaceExisting) {
            LOGGER.error("Path to the destination file is not exists and the file shouldn't be replace.");
            return false;
        }
        try {
            return Files.copy(pathToSourceFile, pathToDestinationFile, StandardCopyOption.REPLACE_EXISTING) != null;
        } catch (IOException e) {
            LOGGER.error("Copying Files failed.", e);
            return false;
        }
    }

    /**
     * Renames a given file
     *
     * @param fromFile The source filename to rename
     * @param toFile   The target fileName
     * @return True if the rename was successful, false if an exception occurred
     */
    public static boolean renameFile(String fromFile, String toFile) {

        try {
            Path src = Paths.get(fromFile);
            return Files.move(src, src.resolveSibling(toFile)) != null;
        } catch (IOException e) {
            LOGGER.error("Renaming Files failed", e);
            return false;
        }
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.<br/>
     * <p>
     * Uses <ul>
     * <li>the default directory associated with the extension of the file</li>
     * <li>the standard file directory</li>
     * <li>the directory of the BIB file</li>
     * </ul>
     *
     * @param databaseContext The database this file belongs to.
     * @param name     The filename, may also be a relative path to the file
     */
    public static Optional<File> expandFilename(final BibDatabaseContext databaseContext, String name,
            FileDirectoryPreferences fileDirectoryPreferences) {
        Optional<String> extension = getFileExtension(name);
        // Find the default directory for this field type, if any:
        List<String> directories = databaseContext.getFileDirectory(extension.orElse(null), fileDirectoryPreferences);
        // Include the standard "file" directory:
        List<String> fileDir = databaseContext.getFileDirectory(fileDirectoryPreferences);
        // Include the directory of the BIB file:
        List<String> al = new ArrayList<>();
        for (String dir : directories) {
            if (!al.contains(dir)) {
                al.add(dir);
            }
        }
        for (String aFileDir : fileDir) {
            if (!al.contains(aFileDir)) {
                al.add(aFileDir);
            }
        }

        return expandFilename(name, al);
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.
     * <p>
     * Will look in each of the given dirs starting from the beginning and
     * returning the first found file to match if any.
     */
    public static Optional<File> expandFilename(String name, List<String> directories) {
        for (String dir : directories) {
            if (dir != null) {
                Optional<File> result = expandFilename(name, dir);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * null if the file does not exist.
     */
    private static Optional<File> expandFilename(String filename, String dir) {

        if ((filename == null) || filename.isEmpty()) {
            return Optional.empty();
        }

        String name = filename;

        File file = new File(name);
        if (file.exists() || (dir == null)) {
            return Optional.of(file);
        }

        if (dir.endsWith(OS.FILE_SEPARATOR)) {
            name = dir + name;
        } else {
            name = dir + OS.FILE_SEPARATOR + name;
        }

        // fix / and \ problems:
        if (OS.WINDOWS) {
            name = SLASH.matcher(name).replaceAll("\\\\");
        } else {
            name = BACKSLASH.matcher(name).replaceAll("/");
        }

        File fileInDir = new File(name);
        if (fileInDir.exists()) {
            return Optional.of(fileInDir);
        } else {
            return Optional.empty();
        }
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
    public static File shortenFileName(File fileName, List<String> dirs) {
        if ((fileName == null) || !fileName.isAbsolute() || (dirs == null)) {
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

    private static File shortenFileName(File fileName, String directory) {
        if ((fileName == null) || !fileName.isAbsolute() || (directory == null)) {
            return fileName;
        }

        String dir = directory;
        String longName;
        if (OS.WINDOWS) {
            // case-insensitive matching on Windows
            longName = fileName.toString().toLowerCase();
            dir = dir.toLowerCase();
        } else {
            longName = fileName.toString();
        }

        if (!dir.endsWith(OS.FILE_SEPARATOR)) {
            dir = dir.concat(OS.FILE_SEPARATOR);
        }

        if (longName.startsWith(dir)) {
            // result is based on original name, not on lower-cased name
            String newName = fileName.toString().substring(dir.length());
            return new File(newName);
        } else {
            return fileName;
        }
    }

    public static Map<BibEntry, List<File>> findAssociatedFiles(List<BibEntry> entries,
            List<String> extensions, List<File> directories, boolean autolinkExactKeyOnly) {
        Map<BibEntry, List<File>> result = new HashMap<>();

        // First scan directories
        Set<File> filesWithExtension = FileFinder.findFiles(extensions, directories);

        // Initialize Result-Set
        for (BibEntry entry : entries) {
            result.put(entry, new ArrayList<>());
        }

        // Now look for keys
        nextFile: for (File file : filesWithExtension) {

            String name = file.getName();
            int dot = name.lastIndexOf('.');
            // First, look for exact matches:
            for (BibEntry entry : entries) {
                Optional<String> citeKey = entry.getCiteKeyOptional();
                if ((citeKey.isPresent()) && !citeKey.get().isEmpty() && (dot > 0)
                        && name.substring(0, dot).equals(citeKey.get())) {
                    result.get(entry).add(file);
                    continue nextFile;
                }
            }
            // If we get here, we did not find any exact matches. If non-exact
            // matches are allowed, try to find one:
            if (!autolinkExactKeyOnly) {
                for (BibEntry entry : entries) {
                    Optional<String> citeKey = entry.getCiteKeyOptional();
                    if ((citeKey.isPresent()) && !citeKey.get().isEmpty() && name.startsWith(citeKey.get())) {
                        result.get(entry).add(file);
                        continue nextFile;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the list of linked files. The files have the absolute filename
     *
     * @param bes list of BibTeX entries
     * @param fileDirs list of directories to try for expansion
     *
     * @return list of files. May be empty
     */
    public static List<File> getListOfLinkedFiles(List<BibEntry> bes, List<String> fileDirs) {
        Objects.requireNonNull(bes);
        Objects.requireNonNull(fileDirs);

        List<File> result = new ArrayList<>();
        for (BibEntry entry : bes) {
            entry.getField(FieldName.FILE).ifPresent(fileField -> {
                List<ParsedFileField> fileList = FileField.parse(fileField);
                for (ParsedFileField file : fileList) {
                    expandFilename(file.getLink(), fileDirs).ifPresent(result::add);
                }
            });
        }

        return result;
    }

    /**
     * Determines filename provided by an entry in a database
     *
     * @param database        the database, where the entry is located
     * @param entry           the entry to which the file should be linked to
     * @param fileNamePattern the filename pattern
     * @param prefs           the layout preferences
     * @return a suggested fileName
     */
    public static String createFileNameFromPattern(BibDatabase database, BibEntry entry,
            String fileNamePattern, LayoutFormatterPreferences prefs) {
        String targetName = null;

        StringReader sr = new StringReader(fileNamePattern);
        Layout layout = null;
        try {
            layout = new LayoutHelper(sr, prefs).getLayoutFromText();
        } catch (IOException e) {
            LOGGER.info("Wrong format " + e.getMessage(), e);
        }
        if (layout != null) {
            targetName = layout.doLayout(entry, database);
        }

        if ((targetName == null) || targetName.isEmpty()) {
            targetName = entry.getCiteKeyOptional().orElse("default");
        }
        //Removes illegal characters from filename
        targetName = FileNameCleaner.cleanFileName(targetName);
        return targetName;
    }

}
