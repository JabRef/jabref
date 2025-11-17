package org.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationkeypattern.BracketedPattern;
import org.jabref.logic.layout.format.RemoveLatexCommandsFormatter;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The idea of this class is to add general functionality that could possibly even in the
 * <a href="https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)">Java NIO package</a>,
 * such as getting/adding file extension etc.
 */
public class FileUtil {

    public static final boolean IS_POSIX_COMPLIANT = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    public static final int MAXIMUM_FILE_NAME_LENGTH = 255;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    private static final String ELLIPSIS = "...";
    private static final int ELLIPSIS_LENGTH = ELLIPSIS.length();
    private static final RemoveLatexCommandsFormatter REMOVE_LATEX_COMMANDS_FORMATTER = new RemoveLatexCommandsFormatter();
    private static final String CYGDRIVE_PREFIX = "/cygdrive/";
    private static final String MNT_PREFIX = "/mnt/";
    private static final Pattern ROOT_DRIVE_PATTERN = Pattern.compile("^/[a-zA-Z]/.*");

    /**
     * MUST ALWAYS BE A SORTED ARRAY because it is used in a binary search
     */
    // @formatter:off
    private static final int[] ILLEGAL_CHARS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
            30, 31, 34,
            42,
            58, // ":"
            60, 62, 63,
            123, 124, 125
    };
    // @formatter:on

    private FileUtil() {
    }

    /**
     * Returns the extension of a file name or Optional.empty() if the file does not have one (no "." in name).
     *
     * @return the extension (without leading dot), trimmed and in lowercase.
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
     * @return the extension (without leading dot), trimmed and in lowercase
     */
    public static Optional<String> getFileExtension(Path file) {
        return getFileExtension(file.getFileName().toString());
    }

    /**
     * @return the name part of a file name (i.e., everything before last ".")
     */
    public static String getBaseName(String fileNameWithExtension) {
        return com.google.common.io.Files.getNameWithoutExtension(fileNameWithExtension);
    }

    /**
     * @return the name part of a file name (i.e., everything before last ".")
     */
    public static String getBaseName(Path fileNameWithExtension) {
        return getBaseName(fileNameWithExtension.getFileName().toString());
    }

    /**
     * Extracts the filename from a URL.
     * If the URL doesn't have a filename (ends with '/'), returns an empty string.
     *
     * @param link the URL string to extract the filename from
     * @return the extracted filename
     */
    public static String getFileNameFromUrl(String link) {
        int slash = link.lastIndexOf('/');
        if (slash >= 0 && slash < link.length()) {
            link = link.substring(slash + 1);
        }
        int query = link.indexOf('?');
        if (query >= 0) {
            link = link.substring(0, query);
        }
        return getValidFileName(link);
    }

    /**
     * Returns a valid filename for most operating systems.
     * <p>
     * It uses {@link FileNameCleaner#cleanFileName(String)} to remove illegal characters.} and then truncates the length to 255 chars, see {@link #MAXIMUM_FILE_NAME_LENGTH}.
     * <p>
     * For "real" cleaning, {@link FileNameCleaner#cleanFileName(String)} should be used.
     */
    public static String getValidFileName(String fileName) {
        String nameWithoutExtension = getBaseName(fileName);

        nameWithoutExtension = FileNameCleaner.cleanFileName(nameWithoutExtension);

        if (nameWithoutExtension.length() > MAXIMUM_FILE_NAME_LENGTH) {
            Optional<String> extension = getFileExtension(fileName);
            String shortName = nameWithoutExtension.substring(0, MAXIMUM_FILE_NAME_LENGTH - extension.map(s -> s.length() + 1).orElse(0));
            LOGGER.info("Truncated the too long filename '{}' ({}} characters) to '{}'.", fileName, fileName.length(), shortName);
            return extension.map(s -> shortName + "." + s).orElse(shortName);
        }

        return fileName;
    }

    /**
     * Adds an extension to the given file name. The original extension is not replaced. That means, "demo.bib", ".sav"
     * gets "demo.bib.sav" and not "demo.sav"
     * <p>
     * <em>Warning: If "ext" is passed, this is literally added. Thus {@code addExtension("tmp.txt", "ext")} leads to "tmp.txtext".</em>
     *
     * @param path      the path to add the extension to
     * @param extension the extension to add
     * @return the modified file name
     */
    public static Path addExtension(Path path, String extension) {
        return path.resolveSibling(path.getFileName() + extension);
    }

    /// Looks for the shortest unique path of the parent directory in the list of paths
    ///
    /// @param paths       List of paths as Strings
    /// @param comparePath The to be tested path
    /// @return Optional.empty() if the paths are disjoint
    public static Optional<String> getUniquePathDirectory(List<String> paths, Path comparePath) {
        // Difference to getUniquePathFragment: We want the parent directory, so we cut off the last path fragment
        return getUniquePathFragment(paths, comparePath)
                .filter(part -> part.contains(File.separator))
                .map(part -> part.substring(0, part.lastIndexOf(File.separator)));
    }

    /// Looks for the shortest unique path in the list of paths
    ///
    /// @param paths       List of paths as Strings
    /// @param comparePath The to be shortened path
    /// @return Shortest unique path fragment (if exists) - Optional.empty() if the paths are disjoint
    public static Optional<String> getUniquePathFragment(List<String> paths, Path comparePath) {
        return uniquePathSubstrings(paths).stream()
                                          .filter(part -> comparePath.toString().contains(part))
                                          .max(Comparator.comparingInt(String::length));
    }

    /**
     * Creates the minimal unique path substring for each file among multiple file paths.
     *
     * @param paths the file paths
     * @return the minimal unique path substring for each file path
     */
    public static List<String> uniquePathSubstrings(List<String> paths) {
        List<Deque<String>> stackList = new ArrayList<>(paths.size());
        // prepare data structures
        for (String path : paths) {
            List<String> directories = Arrays.asList(path.split(Pattern.quote(File.separator)));
            Deque<String> stack = new ArrayDeque<>(directories.reversed());
            stackList.add(stack);
        }

        List<String> pathSubstrings = new ArrayList<>(Collections.nCopies(paths.size(), ""));

        // compute the shortest folder substrings
        while (!stackList.stream().allMatch(Deque::isEmpty)) {
            for (int i = 0; i < stackList.size(); i++) {
                String tempPathString = pathSubstrings.get(i);

                Deque<String> stack = stackList.get(i);

                if (tempPathString.isEmpty() && !stack.isEmpty()) {
                    String stringFromDeque = stack.pop();
                    pathSubstrings.set(i, stringFromDeque);
                } else if (!stack.isEmpty()) {
                    String stringFromStack = stack.pop();
                    pathSubstrings.set(i, stringFromStack + File.separator + tempPathString);
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
     * @return boolean Whether the copy succeeded or was stopped due to the file already existing.
     */
    public static boolean copyFile(Path pathToSourceFile, Path pathToDestinationFile, boolean replaceExisting) {
        // Check if the file already exists.
        if (!Files.exists(pathToSourceFile)) {
            LOGGER.error("Path to the source file doesn't exist.");
            return false;
        }
        if (Files.exists(pathToDestinationFile) && !replaceExisting) {
            LOGGER.error("Path to the destination file exists but the file shouldn't be replaced.");
            return false;
        }
        try {
            // This should also preserve Hard Links
            Files.copy(pathToSourceFile, pathToDestinationFile, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            LOGGER.error("Copying Files failed.", e);
            return false;
        }
    }

    /**
     * Converts an absolute file to a relative one, if possible. Returns the parameter file itself if no shortening is
     * possible.
     * <p>
     * This method works correctly only if directories are sorted decent in their length i.e.
     * /home/user/literature/important before /home/user/literature.
     *
     * @param file        the file to be shortened
     * @param directories directories to check
     */
    public static Path relativize(Path file, List<Path> directories) {
        if (!file.isAbsolute()) {
            return file;
        }
        Optional<Path> realFileOpt = toRealPath(file);

        for (Path directory : directories) {
            if (file.startsWith(directory)) {
                return directory.relativize(file);
            }

            if (realFileOpt.isPresent()) {
                Optional<Path> realDirOpt = toRealPath(directory);
                if (realDirOpt.isPresent()) {
                    Path realFile = realFileOpt.get();
                    Path realDir = realDirOpt.get();
                    if (realFile.startsWith(realDir)) {
                        return realDir.relativize(realFile);
                    }
                }
            }
        }
        return file;
    }

    private static Optional<Path> toRealPath(Path path) {
        if (Files.exists(path)) {
            try {
                return Optional.of(path.toRealPath());
            } catch (IOException e) {
                LOGGER.warn("Could not resolve real path for {}", path, e);
                return Optional.empty();
            }
        } else {
            return Optional.of(path.toAbsolutePath());
        }
    }

    /**
     * Converts an absolute file to a relative one, if possible. Returns the parameter file itself if no shortening is
     * possible.
     *
     * @param path the file path to be shortened
     */
    public static Path relativize(Path path, BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        List<Path> fileDirectories = databaseContext.getFileDirectories(filePreferences);
        return relativize(path, fileDirectories);
    }

    /**
     * Relativizes all BibEntries given to (!) the given database context
     * <p>
     * ⚠ Modifies the entries in the list ⚠
     */
    public static List<BibEntry> relativize(List<BibEntry> entries, BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        List<Path> fileDirectories = databaseContext.getFileDirectories(filePreferences);

        return entries.stream()
                      .peek(entry -> {
                          if (entry.hasField(StandardField.FILE)) {
                              List<LinkedFile> updatedLinkedFiles = entry.getFiles().stream().map(linkedFile -> {
                                  if (!linkedFile.isOnlineLink()) {
                                      String newPath = FileUtil.relativize(Path.of(linkedFile.getLink()), fileDirectories).toString();
                                      linkedFile.setLink(newPath);
                                  }
                                  return linkedFile;
                              }).toList();
                              entry.setFiles(updatedLinkedFiles);
                          }
                      }).toList();
    }

    /**
     * Returns the list of linked files. The files have the absolute filename
     *
     * @param entries      list of BibTeX entries
     * @param fileDirs list of directories to try for expansion
     * @return list of files. May be empty
     */
    public static List<Path> getListOfLinkedFiles(@NonNull List<BibEntry> entries, @NonNull List<Path> fileDirs) {
        return entries.stream()
                      .flatMap(entry -> entry.getFiles().stream())
                      .flatMap(file -> file.findIn(fileDirs).stream())
                      .toList();
    }

    /**
     * Determines filename provided by an entry in a database
     *
     * @param database        the database, where the entry is located
     * @param entry           the entry to which the file should be linked to
     * @param fileNamePattern the filename pattern
     * @return a suggested fileName
     */
    public static Optional<String> createFileNameFromPattern(BibDatabase database, BibEntry entry, String fileNamePattern) {
        String targetName = BracketedPattern.expandBrackets(fileNamePattern, ';', entry, database).trim();

        if (targetName.isEmpty() || "-".equals(targetName)) {
            targetName = entry.getCitationKey().orElse("default");
        }

        if ("default".equals(targetName)) {
            return Optional.empty();
        }

        // Remove LaTeX commands (e.g., \mkbibquote{}) from expanded fields before cleaning filename
        // See: https://github.com/JabRef/jabref/issues/12188
        targetName = REMOVE_LATEX_COMMANDS_FORMATTER.format(targetName);
        // Removes illegal characters from filename
        targetName = FileNameCleaner.cleanFileName(targetName);

        return Optional.of(targetName);
    }

    /**
     * Determines directory name provided by an entry in a database
     *
     * @param database             the database, where the entry is located
     * @param entry                the entry to which the directory should be linked to
     * @param directoryNamePattern the dirname pattern
     * @return a suggested dirName
     */
    public static String createDirNameFromPattern(BibDatabase database, BibEntry entry, String directoryNamePattern) {
        String targetName = BracketedPattern.expandBrackets(directoryNamePattern, ';', entry, database);

        if (targetName.isEmpty()) {
            return targetName;
        }

        // Removes illegal characters from directory name
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
    public static Optional<Path> findSingleFileRecursively(String filename, Path rootDirectory) {
        try (Stream<Path> pathStream = Files.walk(rootDirectory)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().equals(filename))
                    .findFirst();
        } catch (UncheckedIOException | IOException ex) {
            LOGGER.error("Error trying to locate the file {} inside the directory {}", filename, rootDirectory, ex);
        }
        return Optional.empty();
    }

    public static Optional<Path> find(final BibDatabaseContext databaseContext,
                                      @NonNull String fileName,
                                      FilePreferences filePreferences) {
        return find(fileName, databaseContext.getFileDirectories(filePreferences));
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * an empty optional if the file does not exist.
     * <p>
     * Will look in each of the given directories starting from the beginning and
     * returning the first found file to match if any.
     */
    public static Optional<Path> find(String fileName, List<Path> directories) {
        if (directories.isEmpty()) {
            // Fallback, if no directories to resolve are passed
            Path path = Path.of(fileName);
            if (path.isAbsolute()) {
                return Optional.of(path);
            } else {
                return Optional.empty();
            }
        }

        return directories.stream()
                          .flatMap(directory -> find(fileName, directory).stream())
                          .findFirst();
    }

    /**
     * Converts a relative filename to an absolute one, if necessary.
     *
     * @param fileName  the filename (e.g., a .pdf file), may contain path separators
     * @param directory the directory which should be search starting point
     * @return an empty optional if the file does not exist, otherwise, the absolute path
     */
    public static Optional<Path> find(@NonNull String fileName, @NonNull Path directory) {
        if (detectBadFileName(fileName)) {
            LOGGER.error("Invalid characters in path for file {}", fileName);
            return Optional.empty();
        }

        // Explicitly check for an empty string, as File.exists returns true on that empty path, because it maps to the default jar location.
        // If we then call toAbsoluteDir, it would always return the jar-location folder. This is not what we want here.
        if (fileName.isEmpty()) {
            return Optional.of(directory);
        }

        Path resolvedFile = directory.resolve(fileName);
        if (Files.exists(resolvedFile)) {
            return Optional.of(resolvedFile);
        }

        // get the furthest path element from root and check if our filename starts with the same name
        // workaround for old JabRef behavior
        String furthestDirFromRoot = directory.getFileName().toString();
        if (fileName.startsWith(furthestDirFromRoot)) {
            resolvedFile = directory.resolveSibling(fileName);
        }

        if (Files.exists(resolvedFile)) {
            return Optional.of(resolvedFile);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Finds a file inside a list of directory structures. Will also look for the file inside nested directories.
     *
     * @param filename    the name of the file that should be found
     * @param directories the directories that will be searched
     * @return a list including all found paths to files that match the defined conditions
     */
    public static List<Path> findListOfFiles(String filename, List<Path> directories) {
        List<Path> files = new ArrayList<>();
        for (Path dir : directories) {
            FileUtil.find(filename, dir).ifPresent(files::add);
        }
        return files;
    }

    /**
     * Creates a string representation of the given path that should work on all systems. This method should be used
     * when a path needs to be stored in the bib file or preferences.
     */
    public static String toPortableString(Path path) {
        return path.toString()
                   .replace('\\', '/');
    }

    /**
     * Test if the file is a bib file by simply checking the extension to be ".bib"
     *
     * @param file The file to check
     * @return True if file extension is ".bib", false otherwise
     */
    public static boolean isBibFile(Path file) {
        return getFileExtension(file).filter("bib"::equals).isPresent();
    }

    /**
     * Test if the file is a pdf file by simply checking the extension to be ".pdf"
     *
     * @param file The file to check
     * @return True if file extension is ".pdf", false otherwise
     */
    public static boolean isPDFFile(Path file) {
        Optional<String> extension = FileUtil.getFileExtension(file);
        return extension.isPresent() && StandardFileType.PDF.getExtensions().contains(extension.get());
    }

    /**
     * @return Path of current panel database directory or the standard working directory in case the database was not saved yet
     */
    public static Path getInitialDirectory(BibDatabaseContext databaseContext, Path workingDirectory) {
        return databaseContext.getDatabasePath().map(Path::getParent).orElse(workingDirectory);
    }

    /**
     * Detect illegal characters in given filename.
     *
     * @param fileName the fileName to detect
     * @return Boolean whether there is an illegal name.
     * @see org.jabref.logic.util.io.FileNameCleaner#cleanFileName
     */
    public static boolean detectBadFileName(String fileName) {
        // fileName could be a path, we want to check the fileName only (and don't care about the path)
        // Reason: Handling of "c:\temp.pdf" is difficult, because ":" is an illegal character in the file name,
        //         but a perfectly legal one in the path at this position
        try {
            fileName = Path.of(fileName).getFileName().toString();
        } catch (InvalidPathException _) {
            // in case the internal method cannot parse the path, it is surely illegal
            return true;
        }

        for (int i = 0; i < fileName.length(); i++) {
            char c = fileName.charAt(i);
            if (!isCharLegal(c)) {
                return true;
            }
        }
        return false;
    }

    /// Shorten a given file name in the middle of the name using ellipsis. Example: verylongfilenameisthis.pdf
    /// with maxLength = 20 is shortened into verylo...isthis.pdf
    ///
    /// @param fileName  the given file name to be shortened
    /// @param maxLength the maximum number of characters in the string after shortening (including the extension)
    /// @return the original fileName if fileName.length() <= maxLength. Otherwise, a shortened fileName
    public static String shortenFileName(String fileName, Integer maxLength) {
        if (fileName == null || maxLength == null || maxLength < ELLIPSIS_LENGTH) {
            return "";
        }

        if (fileName.length() <= maxLength) {
            return fileName;
        }

        String name;
        String extension;

        extension = FileUtil.getFileExtension(fileName).map(fileExtension -> '.' + fileExtension).orElse("");
        if (extension.isEmpty()) {
            name = fileName;
        } else {
            name = fileName.substring(0, fileName.length() - extension.length());
        }

        int totalNeededLength = ELLIPSIS_LENGTH + extension.length();
        if (maxLength <= totalNeededLength) {
            return fileName.substring(0, maxLength - ELLIPSIS_LENGTH) + ELLIPSIS;
        }

        int charsForName = maxLength - totalNeededLength;
        if (charsForName <= 0) {
            return ELLIPSIS + extension;
        }

        int numCharsBeforeEllipsis;
        int numCharsAfterEllipsis;
        if (charsForName == 1) {
            numCharsBeforeEllipsis = 1;
            numCharsAfterEllipsis = 0;
        } else {
            // Allow the front part to have the extra in odd cases
            numCharsBeforeEllipsis = (charsForName + 1) / 2;
            numCharsAfterEllipsis = charsForName / 2;
        }

        numCharsBeforeEllipsis = Math.min(numCharsBeforeEllipsis, name.length());
        numCharsAfterEllipsis = Math.min(numCharsAfterEllipsis, name.length() - numCharsBeforeEllipsis);

        return name.substring(0, numCharsBeforeEllipsis) +
                ELLIPSIS +
                name.substring(name.length() - numCharsAfterEllipsis) +
                extension;
    }

    public static boolean isCharLegal(char c) {
        return Arrays.binarySearch(ILLEGAL_CHARS, c) < 0;
    }

    /// Converts a Cygwin-style file path to a Windows-style path if the operating system is Windows.
    ///
    /// Supported formats:
    /// - /cygdrive/c/Users/... → C:\Users\...
    /// - /mnt/c/Users/...      → C:\Users\...
    /// - /c/Users/...          → C:\Users\...
    ///
    /// @param filePath the input file path
    /// @return the converted path if running on Windows and path is in Cygwin format; otherwise, returns the original path
    public static Path convertCygwinPathToWindows(String filePath) {
        if (filePath == null) {
            return null;
        }

        if (!OS.WINDOWS) {
            return Path.of(filePath);
        }

        if (filePath.startsWith(MNT_PREFIX) && filePath.length() > 5) {
            return buildWindowsPathWithDriveLetterIndex(filePath, 5);
        }

        if (filePath.startsWith(CYGDRIVE_PREFIX) && filePath.length() > 10) {
            return buildWindowsPathWithDriveLetterIndex(filePath, 10);
        }

        if (ROOT_DRIVE_PATTERN.matcher(filePath).matches()) {
            return buildWindowsPathWithDriveLetterIndex(filePath, 1);
        }

        return Path.of(filePath);
    }

    /// Builds a Windows-style path from a Cygwin-style path using a known prefix index.
    ///
    /// @param path        the input file path
    /// @param letterIndex the index driver letter, zero-based indexing
    /// @return a windows-style path
    private static Path buildWindowsPathWithDriveLetterIndex(String path, int letterIndex) {
        String driveLetter = path.substring(letterIndex, letterIndex + 1).toUpperCase();
        String windowsPath = path.substring(letterIndex + 1).replace("/", "\\\\");
        return Path.of(driveLetter + ":" + windowsPath);
    }
}
