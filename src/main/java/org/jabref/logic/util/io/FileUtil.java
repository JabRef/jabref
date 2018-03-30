package org.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtexkeypattern.BracketedPattern;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.OptionalUtil;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    public static final boolean IS_POSIX_COMPILANT = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    public static final int MAXIMUM_FILE_NAME_LENGTH = 255;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {
    }

    /**
     * Returns the extension of a file name or Optional.empty() if the file does not have one (no "." in name).
     *
     * @return The extension (without leading dot), trimmed and in lowercase.
     */
    public static Optional<String> getFileExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');
        if ((dotPosition > 0) && (dotPosition < (fileName.length() - 1))) {
            return Optional.of(fileName.substring(dotPosition + 1).trim().toLowerCase(Locale.ROOT));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Returns the extension of a file or Optional.empty() if the file does not have one (no . in name).
     *
     * @return The extension, trimmed and in lowercase.
     */
    public static Optional<String> getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    /**
     * Returns the name part of a file name (i.e., everything in front of last ".").
     */
    public static String getBaseName(String fileNameWithExtension) {
        return FilenameUtils.getBaseName(fileNameWithExtension);
    }

    /**
     * Returns a valid filename for most operating systems.
     *
     * Currently, only the length is restricted to 255 chars, see MAXIMUM_FILE_NAME_LENGTH.
     */
    public static String getValidFileName(String fileName) {
        String nameWithoutExtension = getBaseName(fileName);

        if (nameWithoutExtension.length() > MAXIMUM_FILE_NAME_LENGTH) {
            Optional<String> extension = getFileExtension(fileName);
            String shortName = nameWithoutExtension.substring(0, MAXIMUM_FILE_NAME_LENGTH);
            LOGGER.info(String.format("Truncated the too long filename '%s' (%d characters) to '%s'.", fileName, fileName.length(), shortName));
            return extension.map(s -> shortName + "." + s).orElse(shortName);
        }

        return fileName;
    }

    /**
     * Adds an extension to the given file name. The original extension is not replaced. That means, "demo.bib", ".sav"
     * gets "demo.bib.sav" and not "demo.sav"
     *
     * @param path      the path to add the extension to
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
            // Preserve Hard Links with OpenOption defaults included for clarity
            Files.write(pathToDestinationFile, Files.readAllBytes(pathToSourceFile),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;
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
    public static boolean renameFile(Path fromFile, Path toFile) {
        return renameFile(fromFile, toFile, false);
    }

    /**
     * Renames a given file
     *
     * @param fromFile        The source filename to rename
     * @param toFile          The target fileName
     * @param replaceExisting Wether to replace existing files or not
     * @return True if the rename was successful, false if an exception occurred
     */
    public static boolean renameFile(Path fromFile, Path toFile, boolean replaceExisting) {
        try {
            return renameFileWithException(fromFile, toFile, replaceExisting);
        } catch (IOException e) {
            LOGGER.error("Renaming Files failed", e);
            return false;
        }
    }

    public static boolean renameFileWithException(Path fromFile, Path toFile, boolean replaceExisting) throws IOException {
        if (replaceExisting) {
            return Files.move(fromFile, fromFile.resolveSibling(toFile),
                    StandardCopyOption.REPLACE_EXISTING) != null;
        } else {
            return Files.move(fromFile, fromFile.resolveSibling(toFile)) != null;
        }
    }

    /**
     * Converts an absolute file to a relative one, if possible. Returns the parameter file itself if no shortening is
     * possible.
     * <p>
     * This method works correctly only if dirs are sorted decent in their length i.e. /home/user/literature/important before /home/user/literature.
     *
     * @param file the file to be shortened
     * @param dirs directories to check
     */
    public static Path shortenFileName(Path file, List<Path> dirs) {
        if (!file.isAbsolute()) {
            return file;
        }

        for (Path dir : dirs) {
            if (file.startsWith(dir)) {
                return dir.relativize(file);
            }
        }
        return file;
    }

    /**
     * Returns the list of linked files. The files have the absolute filename
     *
     * @param bes      list of BibTeX entries
     * @param fileDirs list of directories to try for expansion
     * @return list of files. May be empty
     */
    public static List<Path> getListOfLinkedFiles(List<BibEntry> bes, List<Path> fileDirs) {
        Objects.requireNonNull(bes);
        Objects.requireNonNull(fileDirs);

        return bes.stream()
                .flatMap(entry -> entry.getFiles().stream())
                .flatMap(file -> OptionalUtil.toStream(file.findIn(fileDirs)))
                .collect(Collectors.toList());
    }

    /**
     * Determines filename provided by an entry in a database
     *
     * @param database        the database, where the entry is located
     * @param entry           the entry to which the file should be linked to
     * @param fileNamePattern the filename pattern
     * @param prefs           the layout preferences
     * @return a suggested fileName
     * @deprecated use String createFileNameFromPattern(BibDatabase database, BibEntry entry, String fileNamePattern ) instead.
     */
    @Deprecated
    public static String createFileNameFromPattern(BibDatabase database, BibEntry entry, String fileNamePattern,
                                                   LayoutFormatterPreferences prefs) {
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

    /**
     * Determines filename provided by an entry in a database
     *
     * @param database        the database, where the entry is located
     * @param entry           the entry to which the file should be linked to
     * @param fileNamePattern the filename pattern
     * @return a suggested fileName
     */
    public static String createFileNameFromPattern(BibDatabase database, BibEntry entry, String fileNamePattern) {
        String targetName = BracketedPattern.expandBrackets(fileNamePattern, ';', entry, database);

        if (targetName.isEmpty()) {
            targetName = entry.getCiteKeyOptional().orElse("default");
        }

        //Removes illegal characters from filename
        targetName = FileNameCleaner.cleanFileName(targetName);
        return targetName;
    }

    /**
     * Determines filename provided by an entry in a database
     *
     * @param database        the database, where the entry is located
     * @param entry           the entry to which the file should be linked to
     * @param fileNamePattern the filename pattern
     * @return a suggested fileName
     */
    public static String createDirNameFromPattern(BibDatabase database, BibEntry entry, String fileNamePattern) {
        String targetName = BracketedPattern.expandBrackets(fileNamePattern, ';', entry, database);

        if (targetName.isEmpty()) {
            targetName = entry.getCiteKeyOptional().orElse("default");
        }

        //Removes illegal characters from filename
        targetName = FileNameCleaner.cleanDirectoryName(targetName);
        return targetName;
    }

    /**
     * Finds a file inside a directory structure. Will also look for the file inside nested directories.
     *
     * @param filename      the name of the file that should be found
     * @param rootDirectory the rootDirectory that will be searched
     * @return the path to the first file that matches the defined conditions
     */
    public static Optional<Path> find(String filename, Path rootDirectory) {
        try (Stream<Path> pathStream = Files.walk(rootDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().equals(filename))
                    .findFirst();
        } catch (UncheckedIOException | IOException ex) {
            LOGGER.error("Error trying to locate the file " + filename + " inside the directory " + rootDirectory);
        }
        return Optional.empty();
    }

    /**
     * Finds a file inside a list of directory structures. Will also look for the file inside nested directories.
     *
     * @param filename    the name of the file that should be found
     * @param directories the directories that will be searched
     * @return a list including all found paths to files that match the defined conditions
     */
    public static List<Path> find(String filename, List<Path> directories) {
        List<Path> files = new ArrayList<>();
        for (Path dir : directories) {
            FileUtil.find(filename, dir).ifPresent(files::add);
        }
        return files;
    }

    /**
     * Creates a string representation of the given path that should work on all systems.
     * This method should be used when a path needs to be stored in the bib file or preferences.
     */
    public static String toPortableString(Path path) {
        return path.toString()
                   .replace('\\', '/');
    }
}
