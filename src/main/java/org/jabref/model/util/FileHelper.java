package org.jabref.model.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.FileDirectoryPreferences;

public class FileHelper {
    private static final Pattern SLASH = Pattern.compile("/");
    private static final Pattern BACKSLASH = Pattern.compile("\\\\");

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
            return Optional.of(fileName.substring(dotPosition + 1).trim().toLowerCase(Locale.ROOT));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns an empty optional if the file does not
     * exist.<br/>
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
    public static Optional<Path> expandFilename(final BibDatabaseContext databaseContext, String name,
                                                        FileDirectoryPreferences fileDirectoryPreferences) {
        Optional<String> extension = getFileExtension(name);
        // Find the default directory for this field type, if any:
        List<String> directories = databaseContext.getFileDirectories(extension.orElse(null), fileDirectoryPreferences);
        // Include the standard "file" directory:
        List<String> fileDir = databaseContext.getFileDirectories(fileDirectoryPreferences);
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
    public static Optional<Path> expandFilename(String name, List<String> directories) {
        for (String dir : directories) {
            if (dir != null) {
                Optional<Path> result = expandFilename(name, dir);
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Converts a relative filename to an absolute one, if necessary. Returns
     * an empty optional if the file does not exist.
     */
    private static Optional<Path> expandFilename(String filename, String directoryName) {
        Objects.requireNonNull(filename);
        Objects.requireNonNull(directoryName);

        Path file = Paths.get(filename);
        if (Files.exists(file)) {
            return Optional.of(file);
        }

        Path directory = Paths.get(directoryName);
        Path resolvedFile = directory.resolve(file);
        if (Files.exists(resolvedFile)) {
            return Optional.of(resolvedFile);
        } else {
            return Optional.empty();
        }
    }
}
